# Europe Trip Planner Agent

A real-world demo of the **Axoniq agentic DSL**: a Gemini-backed agent has a multi-turn
conversation with a human operator, interviewing them about their European trip, then returns
a day-by-day itinerary. Everything — every LLM call, every tool dispatch, every event
publish, every human reply — is a durable workflow step recorded to **Axon Server**.

Crash anywhere mid-conversation and the workflow replays from the last completed step;
Gemini is not re-called for completed turns, the human is not asked questions twice.

## What you get

| Module                  | Purpose                                                                |
|-------------------------|------------------------------------------------------------------------|
| `axoniq-workflow-spring-boot` | Auto-detects `@Workflow` beans and wires them into Axon Server.        |
| `axoniq-workflow-dsl-agent`    | The agentic DSL: `AgenticWorkflowContext`, `runAgent`, built-in `askQuestion`. |
| `axoniq-workflow-dsl-agent-adk`| The Google ADK host adapter — `AdkHostAgent.of(LlmAgent)` + `WorkflowDurabilityPlugin`. |
| `google-adk:google-adk:1.0.0` | Google ADK Java with Gemini support.                                    |

## Prerequisites

1. **Docker** (for Axon Server).
2. **Google AI Studio API key** — get one at <https://aistudio.google.com/apikey>.
3. **Java 21+** & **Maven** (or use the Maven wrapper).

## Run it

```bash
# 1. start Axon Server
docker compose up -d

# 2. supply your Gemini key
export GOOGLE_API_KEY=YOUR_KEY_HERE

# 3. run the app
./mvnw spring-boot:run
```

The app starts on **<http://localhost:9091>**. Axon Server's console is on **<http://localhost:8024>**.

## Use it — UI (recommended)

Open **<http://localhost:9091>** in a browser. A small single-page chat UI is served from
`src/main/resources/static/`:

1. Type a one-line trip brief → *Start planning*.
2. The agent's questions stream in as chat bubbles; type your reply and hit Enter.
3. After the third reply the agent delivers a day-by-day itinerary in the bottom panel.

It polls the same REST endpoints described below every 1.5 s — no WebSockets, no SSE — so you
can refresh the page mid-chat or open a second tab on the same chat id and keep going.

## Use it — REST (curl)

Open three terminals (or use any HTTP client of your choice).

**Start a chat:**
```bash
curl -s -X POST http://localhost:9091/chats \
     -H 'content-type: application/json' \
     -d '{"brief":"A relaxed 10-day European city trip, focus on food and museums."}'
# → {"id":"<chatId>","message":"Trip planning started. Poll /chats/<chatId>/question."}
```

**Watch for the next question (poll every few seconds):**
```bash
curl -s http://localhost:9091/chats/<chatId>/question
# → {"question":"Which European cities or regions would you like to visit?"}
```

**Answer the question:**
```bash
curl -s -X POST http://localhost:9091/chats/<chatId>/answer \
     -H 'content-type: application/json' \
     -d '{"text":"Paris, Rome and Barcelona"}'
```

Repeat the question/answer loop until `/chats/<chatId>/question` returns `204 No Content`.
Then fetch the final itinerary:

```bash
curl -s http://localhost:9091/chats/<chatId>/itinerary
# → {"itinerary":"Day 1-3 Paris: Louvre, Latin Quarter food tour, …"}
```

## What happens under the hood

```
POST /chats                 ──► StartTripPlanning event ──► workflow starts
        │
        ▼
@Workflow execute(ctx)            (one instance per chat id, on Axon Server)
        │
ctx.runAgent("EuropeTripPlanner", definition, brief)
        │  AdkHostAgent.of(llmAgent)
        ▼
ADK Runner with WorkflowDurabilityPlugin
        │
   Gemini  ─► function-call askQuestion("Which cities?")
   BuiltInTools.handle:
        publishAskQuestion[_N]      ──► AskQuestion event
        answer[_N]                  ◄── HumanAnswered event
                                          ▲
   Workflow body                          │
        waitForEvent("question_N", AskQuestion)
        anyMatch(agentRun, question)
        publish HumanAnswered (via reply_N)
                                          │
   Gemini  ─► ... 3 turns total ...
   Gemini  ─► finalText("Day 1-3 Paris …")
        │
        ▼
publishItinerary               ──► TripPlanReady event
workflow COMPLETED
```

Every line of that diagram lands as a durable, replayable step event in Axon Server. Look at
the Axon Server console at <http://localhost:8024> while the workflow is running to see them
stream in: `EuropeTripPlannerStarted`, `ReasonStarted`, `ReasonCompleted{toolCalls:["askQuestion"]}`,
`AskQuestionCallStarted`, `AskQuestionStarted`, `publishAskQuestionStarted`, … and so on.

## Project layout

```
src/main/java/io/axoniq/demo/tripplanner/
├── TripPlannerApplication.java       — Spring Boot main
├── api/
│   ├── StartTripPlanning.java        — trigger event
│   └── TripPlanReady.java            — completion event
├── config/AgenticConfig.java         — wires AgenticWorkflowContextFactory + Gemini supplier
├── controller/ChatController.java    — POST /chats, /answer, GET /question, /itinerary
├── projection/ChatStateProjection.java — projects AskQuestion / HumanAnswered / TripPlanReady
└── workflow/EuropeTripPlannerWorkflow.java — @Workflow using AdkHostAgent + Gemini
src/main/resources/
├── application.yaml                  — port, Axon Server config, Gemini model + API key
└── static/                           — single-page chat UI (vanilla HTML / CSS / JS)
    ├── index.html
    ├── styles.css
    └── app.js
docker-compose.yaml                   — Axon Server 2026.0.0
```

## Swap Gemini for any other model

Change one line in `AgenticConfig.java`:

```java
@Bean
public Supplier<BaseLlm> llmSupplier() {
    return () -> new Gemini("gemini-2.5-flash", apiKey);          // current
    // return () -> new VertexLlm(...);                            // any other ADK BaseLlm
}
```

The workflow body, the tool model, the journaling, the replay semantics — all identical.

## Swap the host SDK entirely

`AdkHostAgent.of(...)` is one of several `HostAgent` implementations. Swap to Spring AI by
changing only the `.hostAgent(...)` line:

```java
.hostAgent(SpringAiHostAgent.of(chatModel))   // ← instead of AdkHostAgent.of(agent)
```

Same workflow, same journal, different model provider.

## Notes on dependencies

The pom pins a few transitive versions to align ADK + Axon Server connector + Spring Boot:

- `protobuf-java` 4.34.0 — ADK gencode is 4.34, runtime must be ≥ gencode.
- `grpc-*` 1.79.0 — ADK transitively brings 1.76, Axon connector wants 1.79; mixing causes
  `ClassNotFoundError` on internal grpc-core classes.
- `jackson-annotations` 2.21 (and `jackson-core` / `databind` 2.21.3) — Jackson 3 introspector
  references `JsonSerializeAs` which only exists in Jackson 2 annotations from 2.21+.

You don't need to think about any of this in normal use — the pom pins them.
