# Multitenancy & Persistent Streams with Axon Server Workshop

This workshop introduces participants to the powerful combination of [multitenancy](https://library.axoniq.io/multitenancy_extension_guide/main/index.html) and [persistent streams](https://library.axoniq.io/axon_framework_ref/events/event-processors/subscribing.html#_persistent_streams) in Axon Server. 
You'll learn how to build scalable, multi-tenant applications, and leverage the efficiency of persistent streams for event processing.

If you have any questions during the workshop, please ask one of the AxonIQ developers walking around the room!
Or, if you are doing this workshop at a later date, [reach out on our Discuss](https://discuss.axoniq.io/).

![https://www.axoniq.io/axoniq-conference-2024](.assets/con24.png)

## Introduction

This repository contains a number of code samples concerning Axon Framework and Axon Server applications. 
The `multitenancy` module is specifically intended to showcase a multi-tenant application using [Axon Framework](https://github.com/AxonFramework/AxonFramework), the [Axon Framework Multi-Tenancy extension](https://github.com/AxonFramework/extension-multitenancy), and [Axon Server](https://www.axoniq.io/products/axon-server).

The multi-tenant application contained in this module is the (by AxonIQ frequently used) Gift Card Management domain. 
As such, it provides a means to issue gift cards, add funds to gift cards, and retrieve the issued cards.
The `GiftCard` aggregate can thus be issued and money can be added to it.
The events are handled by the `giftcard` projection, which saves a gift card with its current remaining amount into a database.
Lastly, this application has a UI through which you can interact with the multitenant Gift Card Management system.

## Getting started

Make sure you have the following tools available:
- Java 21
- Axon Server 2024.2.1
- Docker
- Basic knowledge of Axon Framework and event-driven architectures

From here on out, you can clone the `code-samples` repository:

```bash
git clone git@github.com:AxonIQ/code-samples.git
```

And then checkout the `conference2024` branch, and open the `multitenancy` directory.

### Start Axon Server

We will use Axon Server during the workshop as an event storage engine and the backbone to support multiple tenants.
This module contains a Docker Compose file to start Axon Server:

```bash
docker-compose up -d
```

If you prefer, you can [download a ZIP file with AxonServer as a standalone JAR](https://download.axoniq.io/axonserver/AxonServer.zip).
This will also give you the Axon Server CLI and information on how to run and configure the server.

After starting Axon Server, you should be able to check the dashboard by visiting: [http://localhost:8024](http://localhost:8024)

Upon accessing Axon Server's dashboard, it is **very important** to upload the provided license file to Axon Server in the _License_ tab in the bottom left corner of the dashboard.

## 1 - Create Tenants

The Gift Card Management application is set up such that it is very straightforward to contain several tenants by using the Multitenancy Extension.

> **Task 1**
>
> 1. Start `MultitenancyExampleApplication` application:
> ```bash
> ../mvnw clean spring-boot:run
> ```
> 2. Visit http://localhost:8080.
> 3. Create at least three tenants, for example "Mitchells-Store", "Stevazon", and "Marco's List."

---

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

