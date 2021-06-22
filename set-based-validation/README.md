# Set Based Consistency Validation

A type of question that occurs a lot is `How can I check if an account with a particular email address already exists?`. 
In essence this signals the requirement to be able to validate a set, which is a more intricate process when it comes to applications which follow the CQRS paradigm. Without going in full detail here why this is a problem, a straightforward solution is to enhance the command model by adding a small look-up table which can be queried during such a validation step.

There are several ways to invoke this set validation when using Axon, several of which are shared in this repository.

Prior to checking if the email address already exists you need to save all the email addresses that are used. You can do this by using an event handling component in this case an [AccountEventHandler](https://github.com/AxonIQ/code-samples/blob/master/set-based-validation/src/main/java/io/axoniq/dev/samples/command/handler/AccountEventHandler.java)

You’ll need to make the `AccountEventHandler`'s processing group subscribing to be able to update the repository in the same thread that has applied the event; thus ensuring consistent updates of the set. You can achieve that by adding the following property to your `application.properties` file:
`axon.eventhandling.processors.emailEntity.mode=subscribing`

Subscribing processors will create and maintain a projection immediately after an event has been applied and thus are immediately consistent. Moreover lookup tables like this should always be owned by the command side _only_ and as such should not be exposed through a Query API on top of it.

Now that you have a lookup table you can check if the email address exists before applying the event. We implemented this in three different ways:

1. By using a command message dispatcher which does a check on the CreateAccountCommand e.g. [AccountCreationDispatchInterceptor](https://github.com/AxonIQ/code-samples/blob/master/set-based-validation/src/main/java/io/axoniq/dev/samples/command/interceptor/AccountCreationDispatchInterceptor.java)
2. By using an external commandHandler that does this check for the RequestEmailChangeCommand e.g. [AccountCommandHandler](https://github.com/AxonIQ/code-samples/blob/master/set-based-validation/src/main/java/io/axoniq/dev/samples/command/handler/AccountCommandHandler.java)
3. By using a ParameterResolver that returns a Boolean value that returns true if the email already exists e.g. [EmailAlreadyExistsResolverFactory](https://github.com/AxonIQ/code-samples/blob/master/set-based-validation/src/main/java/io/axoniq/dev/samples/resolver/EmailAlreadyExistsResolverFactory.java)
This way you can add the boolean emailAlreadyExists to the ChangeEmailAddressCommand command handler done in [Account](https://github.com/AxonIQ/code-samples/blob/master/set-based-validation/src/main/java/io/axoniq/dev/samples/command/aggregate/Account.java)

You can test the endpoints using [Swagger](http://localhost:8080/swagger-ui/#)
# References
This implementation is based on https://danielwhittaker.me/2017/10/09/handle-set-based-consistency-validation-cqrs/ 
