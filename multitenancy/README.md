# Workshop: Multitenancy & Persistent Streams with Axon Server
## Axon Conference 2024

This workshop introduces participants to the powerful combination of multitenancy and persistent streams in Axon Server 2024.2.1. You'll learn how to build scalable, multi-tenant applications and leverage the efficiency of persistent streams for event processing.

### Prerequisites

- Java 21
- Axon Server 2024.2.1
- Docker
- Basic knowledge of Axon Framework and event-driven architectures

### Getting Started

1. Clone this repository:

   ```bash
   git clone https://github.com/your-repo/axon-multitenancy-workshop.git
   ```

2. Start Axon Server with Docker Compose:

3. Upload the provided license file to Axon Server in the License Tab of the Axon Server dashboard.
   ```bash
   docker-compose up -d
   ```
4. Run the application:

   ```bash
   ./mvnw clean spring-boot:run
   ```
5. Visit the application UI at `http://localhost:8080`


### Workshop Steps

1. **Create Tenants**: Use the UI to create multiple tenants for our demo system.

2. **Send Commands**: Initiate various actions for different tenants.

3. **Explore Persistent Streams**: Navigate to the Streams page in Axon Server dashboard. Observe how events are processed and confirm all events have been caught up.

4. **Introduce an Error**: Modify the codebase to add a new event handler that throws an error with a specific message.

5. **Observe Error Handling**: Check the UI and Axon Server dashboard to see how the system responds to the introduced error.

6. **Error Resolution and Replay**: Fix the error in the code. Perform a full replay of events for the affected stream to ensure data consistency.

7. **Create a Filtered Stream**: In `application.properties`, define a new stream that only replays events for a specific aggregate ID.

### Additional Resources

- [Introducing Axon Server 2024.1](https://www.axoniq.io/blog/axoniq-server-2024-1)
- [Multitenancy with Axon](https://www.axoniq.io/blog/multitenancy-with-axon)

