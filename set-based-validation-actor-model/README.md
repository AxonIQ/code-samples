# Set Based Consistency Validations using Actor Model

This is an example how use the Actor Model to validate the consistency of the set with Axon Framework.

## How to claim an email address

Let’s assume we got this constraint: `A User can only be registered when the email address is unique.`
We need to create two aggregate types in this case.

The first one is
the [EmailUniquenessCheck Aggregate](src/main/java/io/axoniq/dev/samples/command/aggregate/EmailUniquenessCheck.java),
and the second one is the [User aggregate](src/main/java/io/axoniq/dev/samples/command/aggregate/User.java). The
EmailUniquenessCheck

The `EmailUniquenessCheck` aggregate has the `emailAddress` as the aggregate identifier. Every time a user should be
registered, a command must be sent to the `EmailUniquenessCheck` aggregate to validate the uniqueness.

A command handler should handle this command with a `create_if_missing` creation policy. A boolean property on the
aggregate that keeps the state whether the email
was claimed or not. If the email has not been claimed the email address is approved.

The moment the email address is approved, the user should be created. As you can see, this is done in
the `EmailUniquenessCheck` using the `Aggregatelifecycle.createNew()` function.

If the user registration fails, the email address is not added to the claimed email addresses because the
`EmailAddressApproved` event will not be stored in the event store. And when the email address is not unique, the User
will not be registered. Doing it this way, consistency is guaranteed.

## How to remove release a claim on an email address

Now that we have the creation part covered, we can also try to solve the issue of updating the email address. If someone
wants to change the email address, the former email address should be removed from the existing claimed email addresses.
The good news is that this can be done in two separate transactions.

First, create the user with the new email address. If that command succeeds, the old email address can be claimed again.
The initial value of the claimed boolean is false. When event sourcing the creation event, this value must be set to
true. In the command handler with the `create_if_missing creation` policy, we need to check if the email address has
been claimed. If that is not the case, the email address is allowed and added to the set.

## Composite keys

In this example, I used a String as a unique key, but a custom object can also be used to check a more sophisticated
constraint. An elegant way is to use the object's hash code or concatenate the different values as the aggregate
identifier.

## Testing the behaviour

This [requests-file](src/main/resources/requests.http) contains example requests that you can use to see how this works,
you can start the [DemoApplication](src/main/java/io/axoniq/dev/samples/DemoApplication.java) and see for yourself.

Created with :heart: by [AxonIQ](http://www.axoniq.io)

[axon]: https://axoniq.io/

