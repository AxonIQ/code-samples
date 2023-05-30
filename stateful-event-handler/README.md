# Stateful event processor implementation example

This is an example on how to implement a stateful event processor, which could be used as an alternative to a [Saga](../saga). This use case is implemented in the [OrderProcessor](src/main/java/io/axoniq/dev/samples/orderprocessor/OrderProcessor.java).

A webshop has different bounded contexts, for instance, a checkout (where the customer can add items to the card), an order, a shipment, a payment context. The checkout context will produce events based on actions that the customer does in the checkout. Some of these events are the so-called domain events. These events could trigger actions within other contexts. An example could be the [OrderConfirmedEvent](src/main/java/io/axoniq/dev/samples/order/api/OrderConfirmedEvent.java). When an order is confirmed, the order processing starts: the customer should pay, and the ordered articles need shipping. This process needs to be managed. When the order is not paid you need to take some compensating actions like cancelling the shipment.

This stateful event processor is responsible for the process between multiple bounded contexts. It starts on the [OrderConfirmedEvent](src/main/java/io/axoniq/dev/samples/order/api/OrderConfirmedEvent.java) which marks the start of the order process and contains the `orderId`.

The event-handling methods are annotated with `@EventHandler`. During this process, the [OrderProcess](src/main/java/io/axoniq/dev/samples/orderprocessor/OrderProcess.java) is stored in the database to maintain the state of an order process. In this example JPA is used, but you could use any alternative of your choosing here.

## Stateful event processor configuration example

An event processor will (by default) start its token at the head of the stream. It is possible to change this behavior and let the processor take all historical events into account. The configuration example can be found [here](src/main/java/io/axoniq/dev/samples/config/OrderProcessorConfig.java)
