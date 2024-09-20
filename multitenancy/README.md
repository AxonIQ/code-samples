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

## 5 - Observing flaky Event Handling

Sadly, our Gift Card Management system now has some flaky behavior during event handling.
Luckily, the Persistent Streams provide helpful functionality for just these scenarios.
Before we can show those, we need to ensure exceptions have actually been thrown:

> **Task 5**
>
> 1. Open http://localhost:8080 and http://localhost:8024 side by side.
> 2. Navigate to the "Streams" tab of the "Events" window in the Axon Server dashboard.
> 3. Open up one of your tenants in the Gift Card Management application and click "Send commands."
> 4. What do you see on the "Streams" tab?
> 5. Visit http://localhost:8080/h2-console to check the query models. 
>    The JDBC URL is `jdbc:h2:mem:all-tenants` and the password is `sa`.
> 6. Validate the `GIFT_CARD_ENTITY` table's contents.

---

## 6 - Error Resolution and Persistent Stream Replaying

All nice and well, but we obviously do not want flaky event handlers in our Gift Card Management system.
To fix our projections, we will thus need to solve the bug and replay the events.
As Axon Server is in charge of maintaining the position for Persistent Streams it is capable to reset these to initiate a replay!  

> **Task 6**
>
> 1. Shutdown `MultitenancyExampleApplication`.
> 2. Remove the exceptional code of the `FundsAddedEvent` handler.
> 3. Start `MultitenancyExampleApplication` application:
> ```bash
> ../mvnw clean spring-boot:run
> ```
> 4. Open http://localhost:8024 and visit the "Streams" tab again.
> 5. Click the clock icon in the far right of the faulty tenant.
> 6. Select "Tail of event store - index 0".
> 7. Revisit the database at http://localhost:8080/h2-console.
> 8. Validate that all rows of the `GIFT_CARD_ENTITY` table have a value of _51_.

---

## 7 - Filtered Persistent Streams

Now that we have used Persistent Streams for our multitenant Gift Card Management application and seen its error and reset behavior, it is time to investigate another feature: filtering.
When configuring a Persistent Stream you are able to define the `filter` property.
This `filter` property can take in any [Axon Server Query](https://library.axoniq.io/axon-server-query-language-guide/index.html).
So, for this exercise, let us adjust the properties to only stream events from a single aggregate!

In `application.properties`, define a new stream that only replays events for a specific aggregate identifier.

> **Task 7**
>
> 1. Visit the Axon Server dashboard at http://localhost:8024 
> 2. Open the "Search" page.
> 3. Select one of the contexts that have been created for each of your tenants. For example, "tenant-Stevazon."
> 4. Click the looking class to perform a "search all."
> 5. In the table of events, hover over an "Aggregate Identifier" and click the looking glass icon that pops up.
>    This will automatically construct a query for you, which we can use for our `filter` property. 
> 6. Open the `application.properties` of the multitenant Gift Card Management application.
> 7. Comment the `giftcard-stream1` lines and uncomment the `filtered-stream` lines.
> 8. Copy the Aggregate Identifier query from the Axon Server Dashboard and paste it in the line:
>   `axon.axonserver.persistent-streams.filtered-stream.filter={FILTER_HERE}`
> 8. To trigger the configuration change, shutdown `MultitenancyExampleApplication`.
> 9. Start the `MultitenancyExampleApplication` application again:
> ```bash
> ../mvnw clean spring-boot:run
> ```
> 10. Revisit the database at http://localhost:8080/h2-console and make sure only a single Gift Card is stored.

---

### Additional Resources

- [Introducing Axon Server 2024.1](https://www.axoniq.io/blog/axoniq-server-2024-1)
- [Multitenancy with Axon](https://www.axoniq.io/blog/multitenancy-with-axon)

