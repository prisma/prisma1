---
alias: eiphae7eba 
description: An overview of how Graphcool leverages serverless functions.
---

# Overview

## Combining GraphQL and Serverless Functions

In the previous chapter, we discussed the CRUD GraphQL API that's generated for you based on the model types you specify in your project. In this chapter, we take a closer look at how you can extend the functionality of your backend using *serverless functions*. 

## Runtime Environment

When using Graphcool, there generally are two different ways how you can deploy and invoke your functions:

- Use your own FaaS provider (or any other function provisioning mechanism) to deploy a function and invoke it via an **HTTP webhook**.
- Use the **Graphcool environment** to deploy your function.

Graphcool currently uses AWS Lambda to deploy functions that you define for the Graphcool environment. It is however planned to support other FaaS providers in the future as well so that you can choose any provider you prefer.


## Synchronous vs Asynchronous Functions

On the Graphcool platform, functions are either invoked _synchronously_ or _asynchronously_.

_Synchronous_ functions are most commonly used for transforming and validating data, though there might be other use cases, e.g. related to calling out to 3rd party APIs where awaiting a response is required.  

_Asynchronous_ functions are invoked by _events_ that are happening inside the Graphcool system. An event is always _typed_ (meaning the shape of the data is known upfront) and passed as an input argument to the functions that are associated with it.
