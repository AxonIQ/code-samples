# Spring Cloud Command Routing

This sample shows how the Axoniq Framework Spring Cloud connector distributes commands between the instances of an
application, without Axon Server in between.

Every instance runs the same application. It registers with a Spring Cloud discovery service (Consul, here) and
handles one command, `SayHello`, answering with the id of the instance that handled it:

```
Hello alice, from spring-cloud-command-routing-8082
```

Start several instances and send requests to any of them, and you can see how commands are routed:

1. `SayHello` declares `@Command(routingKey = "name")`. The connector hashes the routing key onto a ring of all
   instances that registered with discovery, so every greeting for the same name is handled by the same instance, no
   matter which instance received the request.
2. Different names are spread over the instances.
3. When an instance stops, only the names it handled move to another instance. The others stay where they were.

There is no configuration specific to Axon: adding `axoniq-springcloud` next to a Spring Cloud discovery
implementation, such as `spring-cloud-starter-consul-discovery`, and a servlet web stack
(`spring-boot-starter-web`), is enough. Instances call each other over HTTP, on the port they registered with.

### Running the application

1. Start Consul. A `docker-compose.yml` is provided for this:

   ```bash
   docker compose up -d
   ```

2. Start three instances of the application, each on its own port, in separate terminals:

   ```bash
   PORT=8081 ../mvnw spring-boot:run
   PORT=8082 ../mvnw spring-boot:run
   PORT=8083 ../mvnw spring-boot:run
   ```

   The Consul UI at [http://localhost:8500](http://localhost:8500) lists the instances as they register.

3. Send greetings through different instances, using the `requests.http` file or `curl`:

   ```bash
   for port in 8081 8082 8083; do curl localhost:$port/hello/alice; echo; done
   for name in alice bob carol dave erin; do curl localhost:8081/hello/$name; echo; done
   ```

   The first loop answers from the same instance three times. The second shows names spread over the instances.

4. Stop one of the instances, wait a few seconds for discovery to notice, and send the same greetings again.

### Spring Boot 3

The sample builds against Spring Boot 4 and Spring Cloud 2025.1 by default. To run it against Spring Boot 3.5 and
Spring Cloud 2025.0 instead, enable the `spring-boot-3` profile:

```bash
PORT=8081 ../mvnw -Pspring-boot-3 spring-boot:run
```
