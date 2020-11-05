# set-based-validation-axon
A question that pops up a lot is How can I check if an account with a particular email address already exists? 
This command side validation could be done with a small look-up table which can be queried during validation.

You can use several ways to validate the set and in this repository you can find a few of them

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

