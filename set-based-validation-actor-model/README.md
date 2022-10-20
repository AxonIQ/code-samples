# Set Based Consistency Validations using Actor Model

This is an example how use the Actor Model to validate the consistency of the set with Axon Framework.

Let’s assume we got this constraint: `A User can only be registered when the email address is unique.`
We need to create two aggregate types in this case. The first one is
the [EmailUniquenessCheck Aggregate](src/main/java/io/axoniq/dev/samples/command/aggregate/EmailUniquenessCheck.java),
and the second
one is the [User aggregate](src/main/java/io/axoniq/dev/samples/command/aggregate/User.java). The EmailUniquenessCheck
Aggregate has the emailAddress as the aggregate identifier.
Every time a user should be registered, a command must be sent to the `EmailUniquenessCheck Aggregate`.
A command handler should handle this command with a `create_if_missing` creation policy. The Aggregate version will be
null.
if an aggregate for the email address does not exist.
The moment the email address is approved, the user should be created. As you can see, this is done in the
EmailConstraintAggregate using the `Aggregatelifecycle.createNew()` function.
If the user registration fails, the email address is not added to the constraints because the EmailAddressApproved event
will not be stored in the event store. And when the email address is not unique, the User will not be registered. Doing
it this way, consistency is guaranteed.

## How to remove a value from the set

Now that we have the creation part covered, we can also try to solve the issue of updating the email address. If someone
wants to change the email address, the former email address should be removed from the existing constraints.
The good news is that this can be done in two separate transactions. First, create the user with the new email address.
If that command succeeds, the old email address can be logically deleted. Because events can not be deleted, we need to
delete the Aggregate logically.
We can delete the Aggregate logically by adding a boolean to the state of the Aggregate. The initial value of this
boolean is false. When event sourcing the deletion event, this value must be set to true. In the command handler with
the `create_if_missing creation` policy, we need to check if the Aggregate is logically deleted. If that is the case,
the email address is allowed and added to the set.

## Composite keys

In this example, I used a String as a unique key, but a custom object can also be used to check a more sophisticated
constraint. An elegant way is to use the object's hash code or concatenate the different values as the aggregate
identifier.

## Testing the behaviour

This [requests-file](src/main/resources/requests.http) contains example requests that you can use to see how this works,
you can start the [DemoApplication](src/main/java/io/axoniq/dev/samples/DemoApplication.java) and see for yourself.

Created with :heart: by [AxonIQ](http://www.axoniq.io)

[axon]: https://axoniq.io/

