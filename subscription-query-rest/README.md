# code-samples

# Subscription Query REST 

In this demo we will try achieve that our REST API feels more native and on sending a command it returns updated projection right away, instead of listening for updates on a different endpoint.

There are two issues that needs to be address:

here are two issues that need to be addressed in this case:

1. We need to subscribe for updates before we send a command, that’s the only way to be sure we will not miss any updates.
Sending commands first and then subscribing for updates will result in race conditions!
There is a simple trick is to subscribe for the initial result first (even that we don’t need it). 
Let’s call it virtual initial result. 
This will open the Subscription query, which will buffer all updates that arrive at this point on.
Since we now have a buffer for updates, we can send a command and after the command has sent we can subscribe to updates flux. If an update arrives after sending a command and before we are subscribed for updates, we will read it automatically from the buffer, therefore we are sure we will not miss any updates.

2. We need to read our own writes, multiple updates/events could be dispatch at the same time, we can’t guarantee order and which one will arrive first.
Without some kind of correlation, we will easily get into trouble and get someone else’s updates.
The Safest way to go is to introduce a unique id for each command.
We can attach this data to event meta-data, and once projection is materialized we can track which command is responsible for this update.
Luckily Axon Framework offers this functionality out of the box.
Every event contains a tracking id which is the id of the command that created that event.
We will use this mechanism to read our own writes.