package io.axoniq.dev.samples.upcaster.json;

import org.axonframework.eventhandling.EventData;
import org.axonframework.eventhandling.GenericDomainEventEntry;
import org.axonframework.messaging.MetaData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class UpcasterTestingUtils {

    public static String extractFileContentsToString(String fileName) {
        InputStream fileInputStream = UpcasterTestingUtils.class.getResourceAsStream(fileName);
        if (fileInputStream == null) {
            throw new IllegalArgumentException("File name [" + fileName + "] is unknown");
        }
        InputStreamReader fileStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
        return new BufferedReader(fileStreamReader)
                .lines()
                .collect(Collectors.joining("\n"));
    }

    public static EventData<?> generateDomainEventData(String payloadType,
                                                       String payloadRevision,
                                                       Object payload) {
        return generateDomainEventData(UUID.randomUUID().toString(), payloadType, payloadRevision, payload);
    }

    public static EventData<?> generateDomainEventData(String aggregateIdentifier,
                                                       String payloadType,
                                                       String payloadRevision,
                                                       Object payload) {
        return new GenericDomainEventEntry<>("aggregateType",
                                             aggregateIdentifier,
                                             0,
                                             UUID.randomUUID().toString(),
                                             Instant.now(),
                                             payloadType,
                                             payloadRevision,
                                             payload,
                                             MetaData.emptyInstance());
    }

    private UpcasterTestingUtils() {
        // Utility class
    }
}
