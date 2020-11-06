# Set Based Consistency Validation
A type of question that occurs a lot is "How can I check if an account with a particular email address already exists?". 
In essence this signals the requirement to be able to validate a set, which is a more intricate process when it comes to applications which follow the CQRS paradigm. Without going in full detail here why this is a problem, a straightforward solution is to enhance the command model by adding a small look-up table which can be queried during such a validation step.

There are several ways to invoke this set validation when using Axon, several of which are shared in this repository.

Prior to checking if the email address already exists you need to save all the email addresses that are used. You can do this by using an event handling component:

https://github.com/YvonneCeelie/set-based-validation-axon/blob/main/src/main/java/com/example/command/handler/AccountEventHandler.java

You’ll need to make this processing group subscribing to update the repository in the same thread as the event has been applied. You can do that by adding this property to your application.properties file:

axon.eventhandling.processors.emailEntity.mode=subscribing

Subscribing processors will create and maintain a projection immediately after an event has been applied and are immediately consistent. These lookup tables are always owned by the command side only and should not be exposed by using a Query API on top of it.

This implementation is based on https://danielwhittaker.me/2017/10/09/handle-set-based-consistency-validation-cqrs/ 
Now that you have a lookup table you can check if the email address exists before applying the event. I implemented this in three different ways:

1. By using a command message dispatcher which does a check on the CreateAccountCommand: https://github.com/YvonneCeelie/set-based-validation-axon/blob/main/src/main/java/com/example/command/interceptor/AccountCreationDispatchInterceptor.java
2. By using an external commandHandler that does this check for the RequestEmailChangeCommand: 
https://github.com/YvonneCeelie/set-based-validation-axon/blob/main/src/main/java/com/example/command/handler/AccountCommandHandler.java
3. By using a ParameterResolver that returns a Boolean value that returns true if the email already exists:
https://github.com/YvonneCeelie/set-based-validation-axon/blob/main/src/main/java/com/example/command/resolver/EmailAlreadyExistsResolverFactory.java
This way you can add the boolean emailAlreadyExists to the ChangeEmailAddressCommand commandhandler:
https://github.com/YvonneCeelie/set-based-validation-axon/blob/main/src/main/java/com/example/command/aggregate/Account.java
