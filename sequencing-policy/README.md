# Custom Sequencing Policy

This demo shows how to set up a custom `SequencingPolicy`.

To that end, a `FlightIdSequencingPolicy` implementation is present. This `SequencingPolicy` implementation first
validates whether it handles a `FlightEvent` implementation. If it does, it returns the `FlightId` as the sequence
identifier. Through this approach, the `FlightIdSequencingPolicy` ensures that all events with the same `FlightId` will
be handled in order within a distributed environment.

The implementation of the `FlightIdSequencingPolicy` is concise because of the common `FlightEvent` used by all flight event implementations.
Without this base event, the `FlightIdSequencingPolicy` would include an if/else or switch statement containing all flight events to achieve the same.

As a bonus, this sample shows how we can achieve the same result with the `PropertySequencingPolicy`.

For more information, read up on sequencing
policies [here](https://docs.axoniq.io/reference-guide/axon-framework/events/event-processors/streaming#sequential-processing).

## Running the application

This sample project is a Spring Boot application, and as such, we can run it as any other Spring Boot application.

It has a simple `/test/bulk/{amount}` endpoint where you can generate a bulk of events.
These events are directly published on the `EventGateway`, resulting in the `FlightTimeProjector` handling them.

The `FlightTimeProjector` in turn logs the thread identifier and flight identifier for every event it handles.
Through this, the console shows that a single thread will handle all events for a given `FlightId` in order.

Since Axon Server is a requirement for this sample, a `docker-compose` file is provided.
Also, if you are on Intellij, a `requests.http` file is provided to make it easy to call the endpoints.
