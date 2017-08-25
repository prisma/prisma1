# Event-Driven Asynchronous Functions

## Overview

Graphcool enables a truly event-driven architecture where the execution of asynchronous functions is treated as a first-class citizen. The benefit coming along with this is a nicely decoupled system with independent pieces of functionality that are highly scalable and cost-effective.

As of today, the event system is based on the mutations provided by the CRUD GraphQL API, meaning that every successful mutation automatically triggers an event that anyone can subscribe to. However, as events really sit at the core of the Graphcool platform, the event system will be extended in the future to allow for more customisation and more ways to pipe events into it.

## Server-side Subscriptions

Graphcool leverages the concept of regular GraphQL subscriptions for its event system. Meaning that clients can *subscribe* to events that they're interested in and be notified whenever that event occurs. 

The benefit that comes from using GraphQL subscriptions for this purpose are threefold:

* events are *typed* and part of the GraphQL schema
* subscribers can specify exactly what data they're interested in when the event occurs by writing a regular subscription query
* reuse of a familiar and powerful concept so developers don't have to learn something new 

Particularly the second point is beneficial and saves a lot of time in the design of an event-driven architecture. Traditionally, a lot of work on the event-producer side would go into deciding on the right level of granularity for an event and thinking about the information it should carry. With GraphQL subscriptions, this is not an issue any more since the even consumer is now in full control of specifying what data it wants to receive upon an occurrence of the event.

A typical use case for server-side subscriptions is calling out to 3rd party APIs or other systems.


## Implement Complex Workflows with State Machines

Sometimes the workflows involved for a certain feature are too complex to be represented by a single asynchronous function call, for example if multiple APIs need to be contacted and the the results of the calls are depending on each other. An example of this might be a scenario where after the a customer ordered an item in a web shop, you have the following sequence of events to be performed:

1. Customer places order
2. Charge customer on Stripe
3. If payment was successful, add data to CRM  (otherwise delete the order and abort)
4. If data was added to CRM, initiate shipping (otherwise retry adding to CRM)
5. If shipping initiation was successful, send confirmation email (otherwise retry initiating shipping)

In such cases, you could either resort to using the `mutationAfter` hook for a synchronous execution where you could call out to all required APIs one after another to account for the dependencies in the workflow. Another, more elegant and performant solution however will be to model your workflow and its dependencies as a *state machine* by adding corresponding fields to your GraphQL types. Every update of a field that's part of the state machine representation will trigger another server-side subscription which in turn will make another call to the corresponding API and update a field on the GraphQL type as a result. Find an example of this [here](http://comingsoon/).


