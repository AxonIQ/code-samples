/*
 * Tiny client for the Europe Trip Planner Agent.
 *
 * Talks to six REST endpoints:
 *   POST   /chats                  — start a new chat
 *   GET    /chats                  — list every known chat (resumable + completed)
 *   GET    /chats/{id}/question    — current pending agent question (204 if none)
 *   GET    /chats/{id}/transcript  — full Q&A history for the chat
 *   POST   /chats/{id}/answer      — submit the operator's reply
 *   GET    /chats/{id}/itinerary   — final itinerary when ready (204 if not yet)
 *
 * Strategy: after starting (or resuming), poll every 1.5s for either a new question or the
 * final itinerary. On page load, fetch the list of known chats — every non-completed one is
 * resumable because the workflow engine rehydrates it from the event store on JVM restart.
 */

const $ = (id) => document.getElementById(id);

const els = {
    resumeCard:   $('resume-card'),
    resumeList:   $('resume-list'),
    showStartBtn: $('show-start-btn'),
    startCard:    $('start-card'),
    chatCard:     $('chat-card'),
    chatId:       $('chat-id'),
    brief:        $('brief'),
    startBtn:     $('start-btn'),
    resetBtn:     $('reset-btn'),
    messages:     $('messages'),
    composer:     $('composer'),
    answer:       $('answer'),
    sendBtn:      $('send-btn'),
    itinerary:    $('itinerary'),
    itineraryTxt: $('itinerary-text'),
    status:       $('status'),
};

const state = {
    chatId:        null,
    pollHandle:    null,
    lastQuestion:  null,
    waitingOnUser: false,
    typingEl:      null,
    finished:      false,
};

// --- UI helpers ---------------------------------------------------------------

function bubble(who, text) {
    const div = document.createElement('div');
    div.className = `bubble ${who}`;
    const label = document.createElement('div');
    label.className = 'who';
    label.textContent = who === 'agent' ? 'agent' : 'you';
    div.appendChild(label);
    const body = document.createElement('div');
    body.textContent = text;
    div.appendChild(body);
    els.messages.appendChild(div);
    els.messages.scrollTop = els.messages.scrollHeight;
}

function setStatus(msg, isError = false) {
    els.status.textContent = msg || '';
    els.status.classList.toggle('error', !!isError);
}

function showTyping() {
    if (state.typingEl) return;
    const div = document.createElement('div');
    div.className = 'bubble agent';
    div.innerHTML = '<div class="who">agent</div><div class="typing"><span></span><span></span><span></span></div>';
    els.messages.appendChild(div);
    els.messages.scrollTop = els.messages.scrollHeight;
    state.typingEl = div;
}

function hideTyping() {
    if (state.typingEl) {
        state.typingEl.remove();
        state.typingEl = null;
    }
}

function showComposer(show) {
    els.composer.classList.toggle('hidden', !show);
    if (show) {
        els.answer.value = '';
        els.answer.focus();
    }
}

function showItinerary(text) {
    els.itinerary.classList.remove('hidden');
    els.itineraryTxt.textContent = text;
}

function showStartCard() {
    els.resumeCard.classList.add('hidden');
    els.chatCard.classList.add('hidden');
    els.startCard.classList.remove('hidden');
    els.brief.focus();
}

function reset() {
    if (state.pollHandle) clearInterval(state.pollHandle);
    state.chatId = null;
    state.lastQuestion = null;
    state.waitingOnUser = false;
    state.finished = false;
    hideTyping();
    els.messages.innerHTML = '';
    els.itinerary.classList.add('hidden');
    els.itineraryTxt.textContent = '';
    showComposer(false);
    setStatus('');
    els.chatId.textContent = '';
    els.brief.value = '';
    // After reset, decide between resume picker and start form based on what's in the store.
    bootstrap();
}

// --- API calls ----------------------------------------------------------------

async function startChat(brief) {
    const resp = await fetch('/chats', {
        method:  'POST',
        headers: {'content-type': 'application/json'},
        body:    JSON.stringify({brief}),
    });
    if (!resp.ok) throw new Error(`POST /chats → ${resp.status}`);
    return resp.json();
}

async function listChats() {
    const resp = await fetch('/chats');
    if (!resp.ok) throw new Error(`GET /chats → ${resp.status}`);
    return resp.json();
}

async function fetchTranscript(id) {
    const resp = await fetch(`/chats/${id}/transcript`);
    if (resp.status === 204) return [];
    if (!resp.ok) throw new Error(`GET /chats/${id}/transcript → ${resp.status}`);
    return resp.json();
}

async function fetchQuestion(id) {
    const resp = await fetch(`/chats/${id}/question`);
    if (resp.status === 204) return null;
    if (!resp.ok) throw new Error(`GET /chats/${id}/question → ${resp.status}`);
    const json = await resp.json();
    return json.question;
}

async function submitAnswer(id, text) {
    const resp = await fetch(`/chats/${id}/answer`, {
        method:  'POST',
        headers: {'content-type': 'application/json'},
        body:    JSON.stringify({text}),
    });
    if (!resp.ok) throw new Error(`POST /chats/${id}/answer → ${resp.status}`);
}

async function fetchItinerary(id) {
    const resp = await fetch(`/chats/${id}/itinerary`);
    if (resp.status === 204) return null;
    if (!resp.ok) throw new Error(`GET /chats/${id}/itinerary → ${resp.status}`);
    const json = await resp.json();
    return json.itinerary;
}

// --- Resume picker ------------------------------------------------------------

async function bootstrap() {
    setStatus('');
    try {
        const chats = await listChats();
        if (!Array.isArray(chats) || chats.length === 0) {
            showStartCard();
        } else {
            renderResumeList(chats);
        }
    } catch (e) {
        // Backend not reachable yet (cold start), endpoint not deployed, or unexpected response
        // — fall back to the start form so the user can always begin a new chat.
        console.warn('bootstrap fell back to start card:', e);
        showStartCard();
    }
}

function renderResumeList(chats) {
    els.startCard.classList.add('hidden');
    els.chatCard.classList.add('hidden');
    els.resumeCard.classList.remove('hidden');
    els.resumeList.innerHTML = '';
    // Pending first, then ready; UUIDs preserve their original order within each group.
    const pending = chats.filter(c => c.status === 'PENDING');
    const ready = chats.filter(c => c.status === 'READY');
    [...pending, ...ready].forEach(chat => {
        const li = document.createElement('li');
        const info = document.createElement('div');
        info.className = 'resume-info';
        const brief = document.createElement('div');
        brief.className = 'resume-brief';
        brief.textContent = chat.brief || '(no brief)';
        info.appendChild(brief);
        const meta = document.createElement('div');
        meta.className = 'resume-meta';
        const metaSuffix = chat.status === 'READY'
                ? ' · itinerary ready'
                : (chat.pendingQuestion ? ` · pending: ${chat.pendingQuestion}` : ' · agent thinking');
        meta.textContent = chat.id.substring(0, 8) + metaSuffix;
        info.appendChild(meta);
        li.appendChild(info);
        const statusBadge = document.createElement('span');
        statusBadge.className = 'resume-status' + (chat.status === 'READY' ? ' ready' : '');
        statusBadge.textContent = chat.status === 'READY' ? 'done' : 'in-flight';
        li.appendChild(statusBadge);
        const btn = document.createElement('button');
        btn.textContent = chat.status === 'READY' ? 'View' : 'Resume';
        btn.addEventListener('click', () => resumeChat(chat.id));
        li.appendChild(btn);
        els.resumeList.appendChild(li);
    });
}

async function resumeChat(id) {
    state.chatId = id;
    state.finished = false;
    state.waitingOnUser = false;
    state.lastQuestion = null;
    els.resumeCard.classList.add('hidden');
    els.startCard.classList.add('hidden');
    els.chatCard.classList.remove('hidden');
    els.chatId.textContent = id.substring(0, 8);
    els.messages.innerHTML = '';
    els.itinerary.classList.add('hidden');
    showComposer(false);
    setStatus('Restoring conversation…');

    try {
        const transcript = await fetchTranscript(id);
        transcript.forEach(entry => bubble(entry.role, entry.text));
        const itinerary = await fetchItinerary(id);
        if (itinerary) {
            // Completed chat — just render the itinerary, no polling.
            state.finished = true;
            showItinerary(itinerary);
            setStatus('Itinerary delivered. ✅');
            return;
        }
        // In-flight: figure out whether we're waiting for the operator or the agent.
        const lastEntry = transcript[transcript.length - 1];
        if (lastEntry && lastEntry.role === 'agent') {
            state.lastQuestion = lastEntry.text;
            state.waitingOnUser = true;
            showComposer(true);
            setStatus('Waiting for your reply…');
        } else {
            showTyping();
            setStatus('Agent is thinking…');
        }
        state.pollHandle = setInterval(poll, 1500);
        poll();
    } catch (e) {
        setStatus(`Error restoring chat: ${e.message}`, true);
    }
}

// --- Polling ------------------------------------------------------------------

async function poll() {
    if (!state.chatId || state.finished) return;
    try {
        const itinerary = await fetchItinerary(state.chatId);
        if (itinerary) {
            state.finished = true;
            hideTyping();
            showComposer(false);
            showItinerary(itinerary);
            setStatus('Itinerary delivered. ✅');
            clearInterval(state.pollHandle);
            return;
        }
        if (state.waitingOnUser) return;

        const question = await fetchQuestion(state.chatId);
        if (question && question !== state.lastQuestion) {
            hideTyping();
            state.lastQuestion = question;
            state.waitingOnUser = true;
            bubble('agent', question);
            showComposer(true);
            setStatus('Waiting for your reply…');
        } else if (!question && !state.lastQuestion) {
            showTyping();
            setStatus('Agent is thinking…');
        }
    } catch (e) {
        setStatus(`Error: ${e.message}`, true);
    }
}

// --- Event wiring -------------------------------------------------------------

els.showStartBtn.addEventListener('click', showStartCard);

els.startBtn.addEventListener('click', async () => {
    const brief = els.brief.value.trim()
        || 'A relaxed European city trip — please plan one.';
    els.startBtn.disabled = true;
    setStatus('Starting chat…');
    try {
        const {id} = await startChat(brief);
        state.chatId = id;
        els.chatId.textContent = id.substring(0, 8);
        els.startCard.classList.add('hidden');
        els.resumeCard.classList.add('hidden');
        els.chatCard.classList.remove('hidden');
        bubble('human', brief);
        showTyping();
        setStatus('Agent is thinking…');
        state.pollHandle = setInterval(poll, 1500);
        poll();
    } catch (e) {
        setStatus(`Error: ${e.message}`, true);
    } finally {
        els.startBtn.disabled = false;
    }
});

els.sendBtn.addEventListener('click', sendAnswer);
els.answer.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendAnswer();
    }
});
els.resetBtn.addEventListener('click', reset);

async function sendAnswer() {
    const text = els.answer.value.trim();
    if (!text || !state.chatId) return;
    els.sendBtn.disabled = true;
    try {
        await submitAnswer(state.chatId, text);
        bubble('human', text);
        showComposer(false);
        state.waitingOnUser = false;
        state.lastQuestion = null;
        showTyping();
        setStatus('Agent is thinking…');
    } catch (e) {
        setStatus(`Error: ${e.message}`, true);
    } finally {
        els.sendBtn.disabled = false;
    }
}

// Initial render: ask the backend what chats are in flight.
bootstrap();
