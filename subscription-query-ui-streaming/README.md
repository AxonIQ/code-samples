# Subscription Query UI Streaming

This demo application aims to showcase how to stream the results from a Subscription Query to the UI.
To that end, this module contains two main packages:

1. `src/main/java` - contains the Axon-based Spring Boot application called `SubscriptionQueryUiStreamingApplication`, using Java 11
2. `src/main/ui` - contains a basic Angular UI that on start up invokes an endpoint of the java application

For more detail on the application, read [this](#java-app) and [this](#angular-app) section for the Java and Angular portion respectively.
If you want to test this sample application, the [How to Run](#how-to-run) section explains how to run it. 

## Java App

The Java application consists out of three mains parts. 

First of these is the `EventPublisher` in the `commandmodel` package. 
The `EventPublisher` periodically publishes a `StreamUpdatedEvent` to spoof an active applications.
It fills the `StreamUpdatedEvent` with `UUIDs`. 

Secondly, the `ModelProjector` in the `querymodel` package is in charge of handling the `StreamUpdatedEvent`.
It adds th e contents to a `List` of strings, and emits an update through the `QueryUpdateEmitter`.
Next to that, a `@QueryHandler` annotated method is present for the `ModelQuery`. 
This query handler returns the entire list of updates. 
The combination of this query handler, and the update emission on the event handler provide an entry point for a subscription query.

Thirdly, the `QueryController` in the `ui` package provides an endpoint on `/app/updates`.
This endpoint returns a `Flux` of `ServerSentEvents`.
The`ServerSentEvents` are filled with the result of a subscription query on the `ModelQuery`. 
As an addition, the controller combines the update stream with a heartbeat stream, ensuring a application shutdown correctly closes the stream.

## Angular App

## How to run

To run this application, both the Java app and the Angular app should be started.
The Java app will require Axon Server as a backing event store.

To run Axon Server, Docker can be used:

```text
docker run -d -p 8024:8024 -p 8124:8124 --name axonserver axoniq/axonserver
```

If desired, Axon Server can also be downloaded in a zip [here](https://download.axoniq.io/axonserver/AxonServer.zip).
The zip file contains a runnable jar that will start Axon Server for you.

With Axon Server in place, the Java application `SubscriptionQueryUiStreamingApplication` can be started.
To start the application, the following command can be run from the main folder:

```text
mvn spring-boot:run
```

This starts the application using port `8080`.
At this stage the application is already publishing events.
The endpoint returning the stream of server sent events can already be tested at `http://localhost:8080/app/updates`.

To run the Angular application, the following command should be executed from the `src/main/ui` folder:

```text
npm start
```

This starts the UI and `http://localhost:4200/`.
Opening the URL we prompt you with the text `App is running!`.
Below this, a new entry is added everytime a new `ServerSentEvent` is received.