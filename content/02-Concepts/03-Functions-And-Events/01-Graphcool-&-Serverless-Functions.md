# Graphcool & Serverless Functions

## The Serverless GraphQL Backend Architecture

Graphcool strives to implement the *Serverless GraphQL Backend Architecture*, a new way of building backends.

There are three core pillars for this architecture:

* An automatically generated GraphQL API for CRUD operations
* Serverless functions for custom business logic
* A global type system determined by the GraphQL schema

The automatically generated GraphQL API was discussed in the previous chapter. In this chapter, we'll take a look at the other two pillars and how they interplay with the Graphcool environment.

At its core, the Serverless GraphQL Backend Architecture offers a new level of abstraction for backend developers. It draws inspiration from the [Twelve-Factor App manifesto](https://12factor.net/) and builds upon the idea of a truly [event-driven](https://martinfowler.com/articles/201701-event-driven.html) application infrastructure.

## Adding Custom Business Logic with Serverless Functions

Most applications will have requirements that go beyond an interface for data storage and the corresponding CRUD operations on Model Types. Often times there are domain-specific rules for data validation and transformation. Clients will want to run custom queries or mutations that can not be covered by simple CRUD operations. An app might also want to call out to third-party APIs or integrate with legacy systems.

For these cases, Graphcool offers an integration layer that's based on *serverless functions*. This layer consists of a number of *hook points* that represent the *lifecycle* of a request as it flows through the Graphcool system. If a hook point is associated with a function that's provided by the developer, this function will be invoked as the request reaches the hook.

In particular, there are four *kinds* of these hook points that can be used implement this custom functionality:

* **HTTP Callbacks:** 
    * `httpRequest`: *Synchronous* validation and transformation of incoming HTTP requests
    * `httpResponse`: *Synchronous* validation and transformation of outgoing HTTP responses 
* **Mutation Callbacks:** 
    * `mutationBefore`: *Synchronous* validation and transformation of the input arguments for a specific mutation
    * `mutationAfter`: *Synchronous* function execution that e.g. allows to call out to 3rd party services after a mutation
* **Server-side Subscriptions:** *Asynchronous* events *after* a mutation was performed
* **Schema Extensions:** *Synchronous* functions that allow for custom queries and mutations as well as “computed” fields; can be thought of as *custom resolvers*

Each of these functions is invoked at a different stage of the request lifecycle and with its own specific context. We'll discuss each kind in the remainder of this chapter.

## Synchronous vs Asynchronous Function Execution

A major distinction of the functions that can be used with Graphcool is whether they are executed in a synchronous or asynchronous fashion.

Synchronous functions are most commonly used for transforming and validating data, though there might be other use cases, e.g. related to calling out to 3rd party APIs where awaiting a response is required. With a synchronous execution, the invoked function has to *return* before the request can travel further through the system. The *output* of a synchronously executed function also determines the *input* for the next stage of the request lifecycle. Notice that synchronous functions also can be *chained*. This means that you can associate multiple functions with one hook point which will then be executed one after another.

Asynchronous functions are invoked by *events* that are happening inside the Graphcool system. An event is always *typed*, i.e. the data that it carries is known upfront, and passed as an input argument to the functions that are associated with it.

## Global Type System Determined by GraphQL Schema

The strongly typed nature of GraphQL allows to clearly define the *input* as well as the *return types* for each function that is set up in the Graphcool system. If you're implementing your functions in a strongly typed language, such as Typescript, Scala or Go, you get the benefit of having type safety already at build-time of your project and prevent your users from running into nasty type-related errors.

## Deploying Functions: Inline & Webhooks

There are two ways how you can integrate a serverless function in a Graphcool project:

* **Inline Functions:** These are written directly in the Graphcool environment and executed there.
* **Webhooks:** When choosing this option, you deploy a function yourself to any available FaaS provider and simply provide the URL at which this function can be invoked

Depending on which of these options you choose, the runtime environment for your function will be different. Graphcool builds on top of [Auth0 Extend](https://auth0.com/extend/) which is where your functions will be executed when you're choosing to use inline functions. In the self-hosted version of Graphcool, there are options to choose your own FaaS provider for inline functions.

Naturally, for webhooks the runtime environment will be determined by the FaaS provider that you chose for the implementation of the functions.
