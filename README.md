# Code Samples

This repository contains several Axon-based sample projects to explain specific topics.
Axon usage extends itself from the repositories under the [Axon Framework project](https://github.com/AxonFramework/),
as well as the tools provided by AxonIQ like [Axon Server](https://www.axoniq.io/products/axon-server).

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
3. [Multitenancy](multitenancy/README.md) - Sample showing 'a' approach to multitenancy.
4. [Reset Handler](reset-handler/README.md) - Sample showing how to reset a `StreamingEventProcessor`.
5. [Saga - No TOAST in PostgreSQL](saga/README.md) - Sample showing a basis Saga, that's stored in a PostgreSQL
   without [TOAST](https://wiki.postgresql.org/wiki/TOAST).
6. [Sequencing Policy](sequencing-policy/README.md) - Sample showing how to set up a custom `SequencingPolicy` to adjust
   the event sequence for a `StreamingEventProcessor`.
7. [Set-Based Validation](set-based-validation/README.md) - Sample showing several approaches to implement set-based
   validation.
8. [Set-Based Validation - Actor Model](set-based-validation-actor-model/README.md) - Sample showing how to implement
   set-based validation through a dedicated aggregate instance.
9. [Snapshots](snapshots/README.md) - Sample showing how to configure aggregate snapshotting.
10. [Stateful Event Handler](stateful-event-handler/README.md) - Sample showing a stateful event handler that can be
    used as a replacement for sagas.
11. [Subscription Query - REST](subscription-query-rest/README.md) - Sample showing how to use Axon's subscription query
    cleanly in a REST-based controller.
12. [Subscription Query - Streaming](subscription-query-streaming/README.md) - Sample showing how to use Axon's
    subscription query cleanly in a streaming-based controller.
13. [Upcasters](upcaster/README.md) - Sample showing several implementations of upcasters.

