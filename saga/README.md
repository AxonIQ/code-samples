# Saga implementation example

This is an example on how to implement a Saga. This use case is implemented in
the [ProcessOrderSaga](src/main/java/io/axoniq/dev/samples/saga/ProcessOrderSaga.java)

A webshop has different bounded contexts, for instance, a checkout (where the customer can add items to the card), an
order, a shipment, a payment context. The checkout context will produce events based on actions that the customer does
in the checkout. Some of these events are the so-called domain events. These events could trigger actions within other
contexts. An example could be
the [OrderConfirmedEvent](src/main/java/io/axoniq/dev/samples/order/api/OrderConfirmedEvent.java). When an order is
confirmed, the order processing starts: the customer should pay, and the ordered articles need shipping. This process
needs to be managed. When the order is not paid you need to take some compensating actions like cancelling the shipment.

A Saga is responsible for the process between multiple bounded contexts. The Saga starts on
the [OrderConfirmedEvent](src/main/java/io/axoniq/dev/samples/order/api/OrderConfirmedEvent.java) which marks the start
of the order process and contains the `orderId`.

An essential aspect of a Saga that is it needs to have an end. An everyday use case is to end a Saga after a certain
amount of time. This is why a deadline is set in the first event handler of this Saga. This deadline is handled in the
method marked with `@DeadlineHandler`.

Sagas are serialized and stored into the database, but the CommandGateway should not be serialized the CommandGateway
because of the large dependency tree. You can prevent the CommandGateway from being serialized by adding the transient
keyword to de definition. If you forget to do so Axon will throw an exception.

The event-handling methods are annotated with `@SagaEventHandler`. The `@StartSaga` annotation will instantiate a new
Saga on every `OrderConfirmedEvent` with a unique orderId. The association property on the `SagaEventHandler` annotation
is mandatory and associates the Saga with the `orderId` on the OrderConfirmedEvent. Other ids like the `shipmentId` and
the `paymentId` are added using the SagaLifeCycle.associateWith() method. The event handlers will be invoked for events
with the same id.

## Saga test example

For testing a test fixture ([ProcessOrderSagaTest](src/test/java/io/axoniq/dev/samples/saga/ProcessOrderSagaTest.java))
can be used. This is a convenient method to implement the tests.
Just define the test subject:  
`private SagaTestFixture<ProcessOrderSaga> testFixture = new SagaTestFixture<>(ProcessOrderSaga.class);`

Inject the resources needed for your Saga (Don't inject Axon resources since they are already injected) :
`testFixture.registerResource(uuidProviderMock);`

And implement any given-when-then style test.

## Saga configuration example

Sagas are backed by an event processor. A saga's event processor will (by default) start its token at the head of the
stream. It is possible to change this behavior and let the processor take all historical events into account. The
configuration example can be found [here](src/main/java/io/axoniq/dev/samples/saga/ProcessOrderSagaConfig.java)

## Serialized saga in PostgreSQL without TOAST

If you are using PostgreSQL the `serialized_saga` column in the saga_entry table will be compressed to OID. If you don't
want that, you can overwrite the dialect
like [this](src/main/java/io/axoniq/dev/samples/dialect/AxonPostgreSQLDialect.java). And add a Hibernate mapping with
an [orm file](src/main/resources/orm.xml). If needed you can migrate the existing Saga entries with
a [sql script](src/main/resources/migrateToBytea.sql)