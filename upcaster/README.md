# Upcasters

This module's intention is to give insights on how to create an upcaster and how to test it. This example uses a Json
serializer.

The FlightDelayedEvent is used as example. The original (null) revision looks like this:

```json
{
  "arrivalTime": "2021-05-27T15:06:10.629267",
  "flightId": "KL123",
  "origin": "LAX",
  "destination": "LON"
}

```

Then a new requirement popped up to put the origin and destination into a separate object called `leg`. Please note, that this is
just an example, and we do recommend keeping your event structure as flat as possible. The upcasted event should look like
this:

```json
{
  "arrivalTime": "2021-05-27T15:06:10.629267",
  "flightId": "KL123",
  "leg": {
    "origin": "LAX",
    "destination": "LON"
  }
}
```

You can find the implementation of the upcaster in
the [FlightDelayedEventUpcaster](src/main/java/io/axoniq/dev/samples/upcaster/FlightDelayedEventUpcaster.java). The
implementation of the test can be
found [here](src/test/java/io/axoniq/dev/samples/upcaster/FlightDelayedEventUpcasterTest.java)

To get this upcaster invoked on the event handler it should be added to
the [EventUpcasterChainFactory](src/main/java/io/axoniq/dev/samples/upcaster/EventUpcasterChainFactory.java) or
annotate it as a Spring component together with an Order annotation.
