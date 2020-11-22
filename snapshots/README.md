# Snapshot
In this demo we will show how to define and configure a Snapshot Trigger for a given Aggregate.

Eventually you will need to speed up the Event Sourcing part of your Aggregate because you already have too many events. That is when a Snapshot is desirable. Axon Framework helps you on that by simply configuring a `SnapshotTriggerDefinition` and instructing your `Aggregate` to use that.

To do it, as said before, you have to configure one or more `SnapshotTriggerDefinition` instances. Axon Framework already provides some implementations, but you are free to implement your own:
1. [NoSnapshotTriggerDefinition](https://github.com/AxonFramework/AxonFramework/blob/master/eventsourcing/src/main/java/org/axonframework/eventsourcing/NoSnapshotTriggerDefinition.java): Implementation that doesn't trigger snapshots at all.
1. [EventCountSnapshotTriggerDefinition](https://github.com/AxonFramework/AxonFramework/blob/master/eventsourcing/src/main/java/org/axonframework/eventsourcing/EventCountSnapshotTriggerDefinition.java): Snapshotter trigger mechanism that counts the number of events to decide when to create a snapshot.
1. [AggregateLoadTimeSnapshotTriggerDefinition](https://github.com/AxonFramework/AxonFramework/blob/master/eventsourcing/src/main/java/org/axonframework/eventsourcing/AggregateLoadTimeSnapshotTriggerDefinition.java): A Snapshotter trigger mechanism which based on the loading time of an Aggregate decides when to trigger the creation of a snapshot.

To make it available for the application to use, you have 2 options. When using Spring Boot AutoConfiguration, just provide it as a `@Bean` like we did in this example [here](./src/main/java/io/axoniq/dev/samples/Application.java).
```
@Bean
public SnapshotTriggerDefinition mySnapshotTriggerDefinition(Snapshotter snapshotter) {
    return new EventCountSnapshotTriggerDefinition(snapshotter, 5);
}
```
and instruct your aggregate to use it like done [here](./src/main/java/io/axoniq/dev/samples/command/MyEntityAggregate.java).
```
@Aggregate(snapshotTriggerDefinition = "mySnapshotTriggerDefinition")
public class MyEntityAggregate {
   // ...
}
```

Or when using Axon Configuration API, you can configure it to your Aggregate like this:
```
AggregateConfigurer<MyEntityAggregate> myEntityAggregateConfigurer =
        AggregateConfigurer.defaultConfiguration(MyEntityAggregate.class)
                           .configureSnapshotTrigger(config -> new EventCountSnapshotTriggerDefinition(
                                   config.snapshotter(), 5
                           ));
Configurer configurer = DefaultConfigurer.defaultConfiguration()
                                         .configureAggregate(myEntityAggregateConfigurer);
```

In both cases, after every 5 events*, a Snapshot will be created. You can check if it was created or not by looking at your logs for the following line:
`o.a.a.c.event.axon.AxonServerEventStore  : Snapshot created`.

> 5 is fine for testing, but in real life you would likely use a threshold between 100 and 250. Important to note is that it will always depend on your Aggregate implementation, so measuring how long it takes to load an Aggregate and basing the count on that will end you up with the most optimal solution.

To test the project, we provide a [requests.http](./requests.http). But you can do the same using curl, like this:

```
curl --data POST "http://localhost:8080/entity/entity-id?name=Entity Name"

curl --data PATCH "http://localhost:8080/entity/entity-id?name=New Name"
```