# Serialization with Apache Avro

This sample demonstrates usage of Apache Avro as the serializer for messaging and storage of events inside Axon Server.
For commands, events, queries, and query responses the Avro schema definitions are provided and are located 
in `src/main/avro` directory. 


Here is an example schema for the `CardIssuedEvent` event:


```avroschema
{
    "type": "record",
    "namespace": "io.axoniq.dev.samples.serializationavro.api",
    "name": "CardIssuedEvent",
    "doc": "A new gift card is issued.",
    "revision": "1",
    "javaAnnotation": "org.axonframework.serialization.Revision(\"1\")",
    "fields": [
        {
            "name": "id",
            "type": "string"
        },
        {
            "name": "amount",
            "type": "int"
        }
    ]
}
```

Using the `avro-maven-plugin`, the Java classes are generated into `target/generated-sources/avro` directory 
during the `generate-sources` build phase. 

The remaining part of the application is a standard implementation of a gift card application, that uses the 
`AvroSerializer` by applying configuration of the serializer using application properties.

```yaml

axon:
  serializer:
    events: avro
    messages: avro
    general: jackson

```

## Running example

To run the example, build it using Apache Maven by running `./mvnw clean install` and then execute the following
command from your command line: `./mvnw -Prun -f serialization-avro`.
If you want to run it from your IDEA IntelliJ, just start the:

`SerializationAvroApplication`

Inspect the `serialization-avro/requests.http` and run the script to exchange some messages. If you want to inspect
the events stored in the AxonServer, please open the AxonServer Dashboard http://localhost:8024/.


