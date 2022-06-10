# Reset Handler
This demo is meant to show how to reset a tracking token.

There are 3 ways of doing that, using Axon Server API, just Axon Framework or the  Axon Server Connector.
A more detailed description can be found in the [reference-guide](https://docs.axoniq.io/reference-guide/axon-server/administration/reset-event-processor-token).

### Using Axon Server REST API

When in a distributed environment, one can have several applications connected to Axon Server while sharing the same token store.
To be able to reset a token in this scenario, we have to ask Axon Server to pause every known instance of a given Processor Name to be able to reset it and start it back again.

> We recommend checking the [RestEventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/service/RestEventProcessorService.java) for more information.

### Using Axon Framework

Axon Framework provides another easy way to do it using the `StreamingEventProcessor` methods, namely `shutDown`, `resetTokens` and `start`. When doing it through Axon Framework, the application instance doing the operation should be the one having the claim of the token.

> We recommend checking the [FrameworkEventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/service/FrameworkEventProcessorService.java) for more information.

## Using the Axon Server Connector
The Axon Server Connector provides methods to pause and restart an event processor.
This functionality can be combined to reset the event processor as shown in the other examples.

> We recommend checking the [ServerConnectorEventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/service/ServerConnectorEventProcessorService.java) for more information.

### Running the application
This is a Spring boot application, as such it can be ran as any other standard Spring Boot application. It has a simple `/event` endpoint where you can create new empty events. For resetting the token, it provides 2 reset endpoints:
- `/server/reset/{processorName}`
- `/framework/reset/{processorName}`

It also provides `/server/start/{processorName}` and `/server/pause/{processorName}` for testing purposes.
  
Since Axon Server is a requirement for this sample, a `docker-compose` file is provided.

Also, if you are on Intellij, a `requests.http` file is provided to make it easy to call the endpoints.

Most of the logic for the Axon Server reset via REST is on the [RestEventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/service/RestEventProcessorService.java) class and the added javadoc should be enough to explain what it does.
In the same way, details for the Axon Server reset via the Server Connector can be found in  [ServerConnectorEventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io/axoniq/service/ServerConnectorEventProcessorService.java).

For the Axon Framework version, we recommend checking the official [StreamingEventProcessor.java](https://github.com/AxonFramework/AxonFramework/blob/master/messaging/src/main/java/org/axonframework/eventhandling/StreamingEventProcessor.java) documentation.

A general introduction, regardless of the method used, can be found in the [reference-guide](https://docs.axoniq.io/reference-guide/axon-server/administration/reset-event-processor-token).