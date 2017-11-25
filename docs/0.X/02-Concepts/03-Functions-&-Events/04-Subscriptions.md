---
alias: ohsoo2soqu 
description: An overview of the three different function types that can be used on the Graphcool platform and how to use them.
---

# Server-side Subscriptions & Events

The last kind of function are server-side subscriptions. In contrast to hooks and custom resolvers, subscriptions are executed _asynchronously_ and triggered by (typed) _events_.


## Events are triggered by database writes

For each database write operation, the GraphQL Engine is going to trigger an event that you can _subscribe_ to by associating it with a serverless function. Once the event is triggered, your function gets invoked by the GraphQL Engine.

When subscribing to an event, you can even specify precisely what data you'd like to receive when the event occurs - using a familiar concept: GraphQL subscription queries. 


## The state machine pattern (Saga) for complex async workflows

Sometimes the workflows involved for a certain feature are too complex to be represented by a single asynchronous function call, for example if multiple APIs need to be contacted and the the results of the calls are depending on each other. An example of this might be a scenario where after the a customer ordered an item in a web shop, you have the following sequence of events to be performed:

1. Customer places order
2. Charge customer on Stripe
3. If payment was successful, add data to CRM  (otherwise delete the order and abort)
4. If data was added to CRM, initiate shipping (otherwise retry adding to CRM)
5. If shipping initiation was successful, send confirmation email & push notification (otherwise retry initiating shipping)

In such cases, you could either resort to using the `operationAfter` hook for a synchronous execution where you could call out to all required APIs one after another to account for the dependencies in the workflow. 

Another, more elegant and performant solution however will be to model your workflow and its dependencies as a _state machine_ by adding corresponding fields to your GraphQL types. Every update of a field that's part of the state machine representation will trigger another server-side subscription which in turn will make another call to the corresponding API and update a field on the GraphQL type as a result.



 




