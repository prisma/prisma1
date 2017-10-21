---
alias: apohpae9ju 
description: An overview of the Graphcool Framework.
---

# The Graphcool Framework

The Graphcool Framework is an open-source backend development framework that builds on modern web techologies. Its powerful abstractions allow for rapid development of event-driven, highly scalable and production-ready GraphQL microservices as well as full-blown backend systems.

At the core of Graphcool is an auto-generated [GraphQL API](!alias-abogasd0go) providing a convenient abstraction layer for your [database](!alias-viuf8uus7o). The API comes with CRUD operations and [realtime](!alias-aip7oojeiv) capabilities, it can eb easily customized with an [API gateway](!alias-ucoohic9zu). Business logic and integrations of existing systems are implemented with serverless [functions](!alias-aiw4aimie9). Out-of-the-box [authentication](!alias-bee4oodood) mechanisms and an expressive [permission](!alias-iegoo0heez) system ensure absolute security for your data.


## A quick history of backend development

### From hardware servers to the cloud

Backend development has undergone tremendous change in the past decades. In the early days of the web, developers and and website providers had to buy and manage their own hardware to deploy and run servers on the internet. The movement towards the cloud removed much of that burden and let developers focus on the applications they wanted to build rather than infrastructure to be managed. 

Web frameworks like [Ruby on Rails](http://rubyonrails.org/), [Express.js](https://expressjs.com/) or [Laravel](https://laravel.com/) (PHP) bootstrap the development of a web server and further contributed to the productivity of backend developers in the past years. 

### Developers still have to manage servers

Even in the cloud, developers still have to manage the servers themselves. These are not running on proprietary hardware any more, instead cloud providers like [AWS](https://aws.amazon.com/), [Google Cloud](https://cloud.google.com/) or [Microsoft Azure](https://azure.microsoft.com/) offer scalable infrastructures where developers can deploy their software to _virtual machines_.

But in the end, developers are still managing servers. This comes with many challenges, like scalability, performance and security concerns, that need to be solved repeatedly.

### Serverless enables developers to focus on their application code

The advent of [_serverless_](https://en.wikipedia.org/wiki/Serverless_computing) technologies introduces a new era where servers are abstracted away. _Serverless functions_ in particular enable a completely new way to architect backend applications.

> The term _serverless_ is a bit misleading. Servers still exist in serverless infrastructures - serverless just means that developers don't have to directly manage them any more. They're abstracted away. Serverless is furthermore characterized by a more effective "pay-as-you-go" cost model where idle servers don't cost anything.

When using serverless functions, developers don't have to implement their own web servers any more that will be deployed to the infrastructure of hosting providers like [Heroku](https://www.heroku.com/) or [Digital Ocean](https://www.digitalocean.com/). Instead, they write code for small units of computation (_functions_) that are deployed and executed in a scalable runtime environment that's offered by a [Functions-as-a-Service](https://en.wikipedia.org/wiki/Function_as_a_service) (FaaS) provider.


## The Graphcool Framework builds on modern web technologies

Graphcool leverages serverless functions as an effective way to build [event-driven](https://martinfowler.com/articles/201701-event-driven.html) backend applications. 

Next to serverless functions, Graphcool's second major pillar is [GraphQL](http://graphql.org/). GraphQL is a new API standard that was invented and [open-sourced by Facebook](https://reactjs.org/blog/2015/02/20/introducing-relay-and-graphql.html) and is now maintained by a large community of individuals and organizations from all over the world.

> Check out [How to GraphQL](https://howtographql.com) to learn everything you need to know about GraphQL.

The Graphcool Framework provides an auto-generated [GraphQL API](!alias-abogasd0go) based on a concrete [data model](!alias-eiroozae8u). The API exposes [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) operations as well as [advanced features](!alias-nia9nushae) like filtering, sorting and pagination.


## Local workflows and a great developer experience

Graphcool backends, called _services_, are managed with the [Graphcool CLI](!alias-zboghez5go).


## How does Graphcool compare to web frameworks like Ruby on Rails or Express.js?

Standard web frameworks like [Ruby on Rails](http://rubyonrails.org/), [Express.js](https://expressjs.com/) or [Laravel](https://laravel.com/) allow you to bootstrap a web server based on a specific programming language. When building a backend application with any of these frameworks, you additionally need to setup and configure a database, integrate an ORM (or implement a custom data layer) and make sure the data is properly returned by the API.

With Graphcool, the aut-generated CRUD API will be the foundation for your backend. This API can also be tailored and easily extended by using an [API gateway](!alias-ucoohic9zu).

Graphcool itself is programming language agnostic. Functions can be implemented in any language that's supported by your function runtime. The API proxy layer effectively is a simple web server that can also be implemented in the language of your choice. You could thus use any of the afore mentioned frameworks to build the proxy layer.












