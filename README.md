# Code Samples

This repository contains several Axon Framework 5-based sample projects to explain specific topics.
Axon usage extends itself from the repositories under the [Axon Framework project](https://github.com/AxonIQ/AxonFramework),
as well as the tools provided by AxoniQ like [Axon Server](https://www.axoniq.io/products/axon-server).

Each Maven module within this project represents a different sample you can use, each with its own `README.md`
explaining the intent and usage.
Some of these modules can actually be run, while others are a plain set-up.
The latter typically are samples that don't require to be ran for additional or much needed explanation.
If a module can be run, you may expect a `docker-compose.yml` to set up the infrastructure in most cases, as well as a "
Spring Boot"-based main class.

Down below is an exhaustive list of all the sample:

1. [Axon-Spring Template](axon-spring-template/README.md) - Sample project providing the scaffolding for a simple Axon-
   and Spring-based application.
2. [Distributed Exceptions](distributed-exceptions/README.md) - Sample showing how to deal with exceptions in a
   distributed application.
3. [Order Fulfillment Workflow](order-fulfillment-workflow/README.md) - Sample showing how to model an Order
   Fulfillment process with the AxoniQ Workflow Engine.
4. [Reset Handler](reset-handler/README.md) - Sample showing how to reset a `PooledStreamingEventProcessor`.
5. [Serialization Avro](serialization-avro/README.md) - Sample showing usage of Apache Avro Commands/Events/Queries.
6. [Sequencing Policy](sequencing-policy/README.md) - Sample showing how to set up a custom `SequencingPolicy` to adjust
   the event sequence for a `PooledStreamingEventProcessor`.
7. [Snapshots](snapshots/README.md) - Sample showing how to configure event-sourced entity snapshotting.
8. [Stateful Event Handler](stateful-event-handler/README.md) - Sample showing a stateful event handler that can be
   used as a replacement for sagas.
9. [Subscription Query - REST](subscription-query-rest/README.md) - Sample showing how to use Axon's subscription query
   cleanly in a REST-based controller.
10. [Subscription Query - Streaming](subscription-query-streaming/README.md) - Sample showing how to use Axon's
    subscription query cleanly in a streaming-based controller.
11. [Workflow Saga](workflow-saga/README.md) - Sample showing a Saga-like process modelled with the AxoniQ Workflow
    Engine.

## Topics now covered by the Axon Framework project itself

A few topics that used to live here as standalone samples now have more thorough, actively maintained examples in the
[AxonIQ/AxonFramework](https://github.com/AxonIQ/AxonFramework) repository's `examples/` directory. Rather than
maintaining a second, thinner copy, we point to those instead:

- **Sagas** - see the [`extension-workflow`](https://github.com/AxonIQ/extension-workflow) project and the automation
  patterns in [`examples/university-demo`](https://github.com/AxonIQ/AxonFramework/tree/main/examples/university-demo).
- **Multitenancy** - see [`examples/university-multi-tenancy-examples`](https://github.com/AxonIQ/AxonFramework/tree/main/examples/university-multi-tenancy-examples).
- **Set-based validation** - see `CourseUniqueNameSetValidation` in [`examples/university-demo`](https://github.com/AxonIQ/AxonFramework/tree/main/examples/university-demo/src/main/java/org/axonframework/examples/demo/university/faculty/write/createcourse).
- **Upcasting / event transformation** - see [`examples/university-message-transformation`](https://github.com/AxonIQ/AxonFramework/tree/main/examples/university-message-transformation).

