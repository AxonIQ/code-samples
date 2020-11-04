# Distributed exceptions
In this demo we will show how to propagate errors as exceptions in a distributed environment.

One of the most common ways to indicate that a logical error has occurred and that Command handling failed is to throw an exception from a
 Command handler. However, if the exception is directly serialized there is no guarantee that the command sending side can properly
  deserialize the
 exception in question. 
 
That is why by default Axon Framework will wrap any exception thrown inside a Command handler into a `CommandExecutionException`.

There are a couple of different approaches on how communicate logical, business errors, and failures between services and one of them is
 shown in this example.

These are some points to pay attention to: 
1. Instead of using generic `IllegalArgumentException` or `IllegalStateException`, different failures can be expressed as exception
 types. In addition to making the code more understandable on both throwing and catching sides, these exceptions can carry more
  information than just a generic message. For example, `NegativeOrZeroAmount` can keep track of the invalid amount.
2. By using an interceptor we can catch the exceptions we are interested in and transform them to API and transport friendly representation.
 In this example, the `ExceptionWrappingHandlerInterceptor` will wrap every domain specific exception to a `GiftCardBusinessError`, which
  is part of the API. Since it's just a simple class carrying the Error code, there's no need to extend any of the exception classes. It's also completely fine to map directly to `GiftCardBusinessErrorCode` without any additional information like messages.
3. The `GiftCardBusinessError` is stored as additional details on the `CommandExecutionException` which will be serialized and returned to the
 command invoking side.
4. On the client side, or the application that sends the command, we need to check which kind of exception occurred. Infrastructural
 exceptions and business errors should be treated differently, so we can check specifically for `CommandExecutionException`. We can now
  extract `GiftCardBusinessError` and parse it and it's error code as we need,
   since it's just a simple POJO that is carrying information.
   
### Running the application
This is a Spring boot application, as such it can be ran as any other standard Spring Boot application. In order to fully demonstrate the
 propagation in a distributed environment, separate profiles have been provided for the client (`rest`) and Command (`command`) sides. In
  order to keep everything simple, Axon Server is used as both the Command bus, and an Event store.
  
1. Ensure that Axon Server is running. A simple `docker-compose` is provided for this purpose.
2. Start two instances of the application, one with `rest` profile and the other with `command` Spring boot profile.
3. Make rest calls from a client of your choice, or use provided examples in `requests.http` file. These can be invoked with IntelliJ IDEA.
