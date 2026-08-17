# Subscription Query REST

In this demo we will try achieve that our REST API feels more native and on sending a command it returns updated
projection right away, instead of listening for updates on a different endpoint.

There are two issues that needs to be address in this case:

1. We need to subscribe for updates before we send a command, that’s the only way to be sure we will not miss any
   updates. Sending commands first and then subscribing for updates will result in race conditions! Axon Framework's
   `QueryGateway#subscriptionQuery(...)` returns a single reactive-streams `Publisher` that combines the (here virtual,
   empty) initial result and every subsequent update: the query is only sent, and the update buffer only opened, once
   that `Publisher` is subscribed to. The simple trick is therefore to subscribe to it first, and only dispatch the
   command once that subscription is established (e.g. from a `doOnSubscribe` callback). If an update arrives right
   after sending the command and before we start consuming from the `Publisher`, we will still read it from the
   buffer that was already open, therefore we are sure we will not miss any updates.

2. We need to read our own writes, multiple updates/events could be dispatch at the same time, we can’t guarantee order
   and which one will arrive first. Without some kind of correlation, we will easily get into trouble and get someone
   else’s updates. The safest way to go is to introduce a unique id for each command. We can attach this data to event
   meta-data, and once projection has materialized we can track which command is responsible for this update. Luckily
   Axon Framework offers this functionality out of the box. Every event contains a tracking id which is the id of the
   command that created that event. We will use this correlation mechanism to read our own writes.