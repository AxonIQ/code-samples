package io.axoniq.dev.samples.serializationavro;

import io.axoniq.dev.samples.serializationavro.api.Card;
import io.axoniq.dev.samples.serializationavro.api.CardIssuedEvent;
import io.axoniq.dev.samples.serializationavro.api.CardList;
import io.axoniq.dev.samples.serializationavro.api.CardRedeemedEvent;
import io.axoniq.dev.samples.serializationavro.api.GetAllCardsQuery;
import io.axoniq.dev.samples.serializationavro.api.GetCardByIdQuery;
import io.axoniq.dev.samples.serializationavro.api.IssueCardCommand;
import io.axoniq.dev.samples.serializationavro.api.RedeemCardCommand;
import org.apache.avro.message.SchemaStore;
import org.axonframework.spring.serialization.avro.AvroSchemaScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@AvroSchemaScan
public class SerializationAvroApplication {

    public static void main(String[] args) {
        System.setProperty("disable-axoniq-console-message", "true");
        SpringApplication.run(SerializationAvroApplication.class, args);
    }

    @Profile("manualSchemaStore")
    @Bean("defaultAxonSchemaStore")
    public SchemaStore defaultAxonSchemaStore() {
        var cache = new SchemaStore.Cache();
        cache.addSchema(Card.SCHEMA$);
        cache.addSchema(CardIssuedEvent.SCHEMA$);
        cache.addSchema(CardList.SCHEMA$);
        cache.addSchema(CardRedeemedEvent.SCHEMA$);
        cache.addSchema(GetAllCardsQuery.SCHEMA$);
        cache.addSchema(GetCardByIdQuery.SCHEMA$);
        cache.addSchema(IssueCardCommand.SCHEMA$);
        cache.addSchema(RedeemCardCommand.SCHEMA$);
        return cache;
    }
}
