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

## 2 - Create Gift Cards and Add Funds

We are now inclined to fill up the gift cards for each of the newly created tenants. 
To that end, we will issue several cards and add funds to some of them, for all tenants, by using the provided "Send commands" operation in the UI.
Each click of "Send commands" button will issue a single gift card with one euro on it.
Following the `IssueCardCommand`, the operation will send forty-nine `AddsFundsCommands` adding one euro to the card each, totalling the `remainingValue` of the gift card to 50,- euros.

> **Task 2**
>
> 1. Visit http://localhost:8080.
> 2. Select one of your tenants and click "Send commands".
> 3. Click "or go back" and repeat step 2 until performed for each tenant.

---

## 3 - Explore Persistent Streams

Now that the gift card management application contains several tenants with "actual" gift cards in them, we can start taking a look at the [Persistent Stream](https://library.axoniq.io/axon_framework_ref/events/event-processors/subscribing.html#_persistent_streams) functionality of Axon Server and Axon Framework. 
Hence, we will navigate to the Persistent Streams page and observe that all the event handlers have been caught up.

> **Task 3**
>
> 1. Visit http://localhost:8024.
> 2. Open the "Events" window.
> 3. Observe the "Streams" tab.
> 4. Have all the segments been caught up?

---

## 4 - Flaky Event Handlers

Now that we are aware that our Event Handlers using Persistent Streams are caught up, let us introduce some errors in the system.
We are going to adjust the `FundsAddedEvent` handler in the `GiftCardHandler` such that it will (sometimes) throw an exception.

> **Task 4**
>
> 1. Shutdown `MultitenancyExampleApplication`.
> 2. Open the `GiftCardHandler` file in the `query` package and navigate to the `FundsAddedEvent` handler.
> 3. Adjust the `FundsAddedEvent` handler such that it throws an exception. For example, based on a randomly generated value.
> 4. Start `MultitenancyExampleApplication` application:
> ```bash
> ../mvnw clean spring-boot:run
> ```

---

### Additional Resources

- [Introducing Axon Server 2024.1](https://www.axoniq.io/blog/axoniq-server-2024-1)
- [Multitenancy with Axon](https://www.axoniq.io/blog/multitenancy-with-axon)

