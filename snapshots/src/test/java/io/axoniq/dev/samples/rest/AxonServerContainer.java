package io.axoniq.dev.samples.rest;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AxonServerContainer {

    private AxonServerContainer() {
        // utility class
    }

    public static GenericContainer<?> startAxonServer(String version) {
        GenericContainer<?> axonServer =
                new GenericContainer<>("axoniq/axonserver:" + version)
                        .withExposedPorts(8124, 8024)
                        .waitingFor(Wait.forHttp("/actuator/health").forPort(8024));
        axonServer.start();
        return axonServer;
    }
}
