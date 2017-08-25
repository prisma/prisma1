# Synchronous Functions for Transforming & Validating Data

## Overview

Graphcool offers several hook points that can be used for data validation and transformation. Potential use cases are validating email addresses, formatting phone numbers or transforming the payload that is returned to a client in a specific way.

There are two levels on which validation and transformation can be performed:

* HTTP
* Database transaction

For both levels, there is one *before* and one *after* hook point. 

On the HTTP level, this means that you can validate and/or transform the incoming, plain HTTP request. This is done with the *before* hook point, called `httpRequest`. Similarly, you'll be able to make changes to the response that was packaged up by the Graphcool system before it's returned to the client. This happens in the `httpResponse` step.

Database transactions are typically performed in the context of a *GraphQL mutation*. The corresponding hook points are thus called `mutationBefore` and `mutationAfter`. Both hook points will be invoked for each write to the database and allow you to validate and/or make changes to the data that is being persisted.

One thing to note about Mutation Callbacks is that it is a common scenario that one single HTTP request actually leads to multiple database transactions. This can be the case if the request contains multiple mutations at once or a nested mutation. Consequently, there might be multiple invocations of `mutationBefore` and `mutationAfter` for one incoming request. 

## `httpRequest`

The `httpRequest` hook allows you to inspect the HTTP requests that are coming in. At this point, you can *validate* the request, meaning you could go and reject the request if anything in it violates your domain-specific requirements. Another thing you might do at this stage is *transform* the contents of the request before they're getting passed on to the next layer. 

Functions associated with the `httpRequest` hook are executed *synchronously*, so that the request will only reach the next phase in its lifecycle after the function has returned. It's also possible to associate multiple functions with this hook which will be executed one after another, also called *chained *functions. When this is the case, the output of the previous function will determine the input of the next one. 

When associating a function with this hook point, the function will be invoked for *every* incoming HTTP request, no matter what it contains or where it comes from. Putting too much functionality here thus might slow down your system if being overloaded. Possible use cases for `httpRequest` might include request logging or blocking users with certain IP addresses.     

## `httpResponse`

`httpResponse` on the other hand can be used to apply some final transformations to the response before it is returned to the client. Notice that this hook needs to be used with care as you can potentially change the server's response (or its payload) so that it doesn't adhere to the client's expectations any more. As an example, you're able to remove a field from the payload that the server was about to return to a client. If the client specified this field in the query, you're effectively breaking the GraphQL specification.

Similar to `httpRequest`, this function will be invoked for *all* outgoing responses and thus should only be used for truly global functionality.


## `mutationBefore`

`mutationBefore` is invoked right before each database transaction. It receives the mutation arguments as its input and can modify them before they get persisted or potentially reject the whole mutation (even before the the actual database transaction began).

This hook is a great place to make sure the data that's written to the database adheres to your formatting requirements or other application-specific constraints you might have.  

## `mutationAfter`

The functions associated with the `mutationAfter` hook are invoked right after the database transaction has finished and the belonging data was persisted. They get passed the same input as the functions invoked at `mutationBefore`. At this stage, it's still possible to return an error to the client, however, the DB transaction has finished 

The primary use case for `mutationAfter` is calling out to a 3rd party API and awaiting a response before returning data to the client. You could imagine a payment process where you want to charge a user on Stripe after they placed an order (maybe in the form of `createOrder`-mutation) and include the status of the payment in the response.

