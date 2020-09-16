# code-samples

# Subscription Query REST 

## Eventual consistency

In this demo we will try to solve eventual consistency in CQRS system.
What we want to achieve is that our REST API feels native and on sending a command it returns updated projection right away, instead of listening for updates on a different endpoint.

There are several issues that needs to be address:

1. We need to subscribe to updates same time before we send a command, that's the only way to be sure we will not miss any updates. Sending commands first and then subscribing for updates will result with race condition! 
In this example trick is to subscribe to initial result first, even that we don't need it. Let's call it `virtual initial result`. This will open Subscription query, that will buffer all updates that arrive at this point.
Since we now have a buffer for updates, we can send a command and after command has sent we can subscribe to `updates` flux. If update arrives after sending a command and before we are subscribed for updates, we will read it automatically from buffer, therefore we are sure we will not miss any updates. 
2. We need to read our own writes, multiple updates/events could be dispatch in same time, we can't guarantee order and which one will arrive first. Without some kind of correlation we will easily get into trouble and get someone else's update. The Safest way to go is to introduce unique id for each command. We can attach this data to event meta-data, and once projection is materialized we can track which command is responsible for this update. Luckily Axon Framework offers this functionality out of the box. Every event contains tracing id which is id of command that created that event. We will use this mechanism to force consistency.  
