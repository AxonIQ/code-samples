# Snapshot

In this demo we will show how to define and configure Snapshotting for a given event-sourced entity.

Eventually you will need to speed up the Event Sourcing part of your entity because the number of events exceeds your
defined loading time. That is when a Snapshot is desirable. Axon Framework helps you on that by simply configuring a
`SnapshotPolicy` and instructing your entity to use it.

Snapshotting is enabled by providing a `SnapshotPolicy` for an event-sourced entity. Axon Framework already provides
some standard policies, but you are free to implement your own:

1. `SnapshotPolicy.afterEvents(int)`: Triggers a snapshot once the entity has evolved through a configurable number
   of events. This is the entity-conversion of the former `EventCountSnapshotTriggerDefinition`.
2. `SnapshotPolicy.afterSourcingTime(Duration)`: Triggers a snapshot when the time to source an entity exceeds a
   configured threshold. This is the entity-conversion of the former `AggregateLoadTimeSnapshotTriggerDefinition`.
3. `SnapshotPolicy.whenEventMatches(Predicate)`: Triggers a snapshot when a specific event is encountered during
   sourcing, allowing snapshots to be forced for domain-specific events.

These policies can be combined (e.g. `policyA.or(policyB)`) to form more advanced strategies.

To make it available for the application to use, you have 2 options. When using Spring Boot AutoConfiguration, the
simplest option is to add the `@Snapshotting` annotation directly to your event-sourced entity, alongside
`@EventSourced`, like done [here](./src/main/java/io/axoniq/dev/samples/command/MyEntity.java).

```java
@EventSourced(tagKey = "MyEntity")
@Snapshotting(afterEvents = 5)
class MyEntity {
    // ...
}
```

Or when using the Axon Configuration API directly, you can configure the `SnapshotPolicy` on the
`EventSourcedEntityModule` like this:

```java
EventSourcedEntityModule<String, MyEntity> module =
        EventSourcedEntityModule.declarative(String.class, MyEntity.class)
                                 // ...
                                 .snapshotPolicy(config -> SnapshotPolicy.afterEvents(5));
```

In both cases, after every 5 events, a Snapshot will be created. You can check if it was created or not by looking at
your logs, or by inspecting the configured `SnapshotStore`.

> 5 is fine for testing, but in real life you would likely use a threshold between 100 and 250. Important to note is
> that it will always depend on your entity implementation, so measuring how long it takes to load an entity and
> basing the count on that will end you up with the most optimal solution.

Snapshots are persisted through a `SnapshotStore`. When using Axon Server as your event store (the default for this
sample), an `AxonServerSnapshotStore` is automatically provided, so no additional configuration is required. For
tests or simple setups you can register an `InMemorySnapshotStore` instead.

Because a snapshot captures the state of the entity itself, that state needs to be (de)serializable by the
application's `Converter`. This sample relies on the default `JacksonConverter`, which is why
`MyEntity` still exposes its state through package-private `@JsonGetter`/`@JsonSetter` accessors - the
same requirement that existed under Axon Framework 4's `Serializer` model.

> Snapshots created under Axon Framework 4 cannot be reused after migrating to Axon Framework 5: the event
> numbering scheme used to determine a snapshot's position in the stream has changed. This is expected, and of no
> concern for a sample application without persisted data - a fresh snapshot is simply created again after the
> configured number of events.

To test the project, we provide a [requests.http](./requests.http). But you can do the same using curl, like this:

```
curl --data POST "http://localhost:8080/entity/entity-id?name=Entity Name"

curl --data PATCH "http://localhost:8080/entity/entity-id?name=New Name"
```
