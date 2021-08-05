# Reset Handler
This demo is meant to show how to reset a tracking token using Axon Server API.

When in a distributed environment, one can have several applications connected to Axon Server while sharing the same token store.
To be able to reset a token in this scenario, we have to ask Axon Server to pause every known instance of a given Processor Name to be able to reset it and start it back again.
   
### Running the application
This is a Spring boot application, as such it can be ran as any other standard Spring Boot application. It has a simple `/event` endpoint where you can create new empty events but also a `/start`, `/pause` and `/reset` ones.

> We recommend checking the [EventProcessorRestController.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io.axoniq/EventProcessorRestController.java) for more information.
  
Since Axon Server is a requirement for this sample, a `docker-compose` file is provided.

Also, if you are on Intellij, a `requests.http` file is provided to make it easy to call the endpoints.

Most of the logic is on the [EventProcessorService.java](https://github.com/AxonIQ/code-samples/blob/master/reset-handler/src/main/java/io.axoniq/EventProcessorService.java) class and the added javadoc should be enough to explain what it does.