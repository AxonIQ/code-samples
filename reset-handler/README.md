# Reset Handler

This demo is meant to show how to reset a tracking token.

There are 2 ways of doing that, using Axon Server API or just Axon Framework.

### Using Axon Server API

When in a distributed environment, one can have several applications connected to Axon Server while sharing the same
token store.
To be able to reset a token in this scenario, we have to ask Axon Server to pause every known instance of a given
Processor Name to be able to reset it and start it back again.

> We recommend checking
> the [ServerEventProcessorRestController.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/server/ServerEventProcessorRestController.java)
> for more information.

### Using Axon Framework

Axon Framework provides another easy way to do it using the `StreamingEventProcessor` methods,
namely `shutdown`, `resetTokens` and `start`. Since Axon Framework 5, `TrackingEventProcessor` has been removed in
favor of `PooledStreamingEventProcessor`, which is the only implementation of `StreamingEventProcessor` you'll
encounter, and these methods are now asynchronous, returning a `CompletableFuture` instead of blocking. When doing
it through Axon Framework, the application instance doing the operation should be the one having the claim of the
token.

> Note: as of Axon Framework 5, tokens carry a `mask` used to support (un)claiming individual segments. This sample
> has no meaningful data to preserve across a reset, so it always performs a full reset to the start of the event
> stream (`resetTokens()`) rather than migrating an existing token's mask. If you're adapting this sample to a real
> application with an existing token store, consult the Axon Framework 5 migration guide for the token store schema
> changes before resetting tokens that must retain their claim/mask state.

> We recommend checking
> the [FrameworkEventProcessorRestController.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/framework/FrameworkEventProcessorRestController.java)
> for more information.

### Running the application

This is a Spring boot application, as such it can be ran as any other standard Spring Boot application. It has a
simple `/event` endpoint where you can create new empty events. For resetting the token, it provides 2 reset endpoints:

- `/server/reset/{processorName}`
- `/framework/reset/{processorName}`

It also provides `/server/start/{processorName}` and `/server/pause/{processorName}` for testing purposes.

Since Axon Server is a requirement for this sample, a `docker-compose` file is provided.

Also, if you are on Intellij, a `requests.http` file is provided to make it easy to call the endpoints.

Most of the logic for the Axon Server reset is on
the [EventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/server/EventProcessorService.java)
class and the added javadoc should be enough to explain what it does.

For the Axon Framework version, we recommend checking the
official [StreamingEventProcessor.java](https://github.com/AxonFramework/AxonFramework/blob/main/messaging/src/main/java/org/axonframework/messaging/eventhandling/processing/streaming/StreamingEventProcessor.java)
documentation.
