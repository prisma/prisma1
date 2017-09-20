---
alias: apohpae9ju 
description: An overview of the Graphcool platform.
next: There is more to come
---

# What is Graphcool

Graphcool is a modern backend development platform leveraging GraphQL and serverless techologies. It consists of multiple architectural components, like a GraphQL engine, an event gateway as well as an (optional) API proxy layer. Depending on your use case, you can choose to pick the parts that help you build your own backend easily or go with a full-blown hosted solution.


## A quick history of backend development

### From hardware servers to the cloud

Backend development has undergone tremendous change in the past decades. In the early days of the web, developers and and website providers had to buy and manage their own hardware to deploy and run servers on the internet. The movement towards the cloud removed much of that burden and let developers focus on the applications they wanted to build. 

Web frameworks like [Ruby on Rails](http://rubyonrails.org/), [Express.js](https://expressjs.com/) or [Laravel](https://laravel.com/) (PHP) bootstrap the development of a web server and further contributed to the productivity of backend developers in the past years. 


### Developers still have to manage servers

Even in the cloud, developers still have to manage the servers themselves. These are not running on proprietary hardware any more, instead cloud providers like [AWS](https://aws.amazon.com/), [Google Cloud](https://cloud.google.com/) or [Microsoft Azure](https://azure.microsoft.com/) offer scalable infrastructures where developers can deploy their software to _virtual machines_.

But in the end, developers are still managing servers. This comes with many challenges, like scalability, performance and security concerns, that need to be solved repeatedly.


### Serverless enables developers to focus on their application code

The advent of [_serverless_](https://en.wikipedia.org/wiki/Serverless_computing) technologies introduces a new era where servers are abstracted away. _Serverless functions_ in particular enable a completely new way to architect the backend for an application.

> The term _serverless_ is a bit misleading. Servers still exist in serverless infrastructures - serverless just means that developers don't have to directly manage them any more. They're abstracted away. Serverless is furthermore characterized by a more effective "pay-as-you-go" cost model where idle servers don't cost anything. 

When using serverless functions, developers don't have to implement their own web servers any more that will be deployed to the infrastructure of hosting providers like Heroku or Digital Ocean. Instead, they write code for small units of computation (_functions_) that are deployed and executed in a scalable runtime environment that's offered by a Functions-as-a-Service (FaaS) provider.


## Graphcool is built on modern web technologies

Graphcool leverages serverless functions as an effective way to implement business logic in an application. Several different kinds of functions can be implemented to hook into the GraphQL engine, asynchonously react to events that are happening in the Graphcool system or add functionality to the API.

Next to serverless, Graphcool's second major pillar is [GraphQL](http://graphql.org/). GraphQL is a new API standard that was invented and open-sourced by Facebook and is now maintained by a large community of individuals and organizations from all over the world.

The core of the Graphcool platform is the GraphQL engine. It implements a mapping between database and API by exposing the database operations through GraphQL. It also contains an event gateway that can be used to build event-driven applications.


## Is Graphcool a Backend-as-a-Service?

No! Graphcool is a _backend development platform_, meaning it offers different components to backend developers so that they can build their own backend. Nonetheless, frontend and mobile developers who don't want to spend too much time on the server-side can choose the hosted version of Graphcool where they effectively get a production-ready GraphQL backend right off the bat, thus coming very close to what's commonly understood under the idea of a BaaS.


## How does Graphcool compare to web frameworks like Ruby on Rails or Express.js?

Standard web frameworks like [Ruby on Rails](http://rubyonrails.org/), [Express.js](https://expressjs.com/) or [Laravel](https://laravel.com/) allow you to build a web server in a specific programming language. When using any of these frameworks, you need to setup and configure a database, integrate an ORM or implement a custom data access layer and make sure the data is properly returned by the API.

With Graphcool, the GraphQL engine will be the foundation for your backend. Depending on which parts of the application you'd like to "control" yourself, you can choose to go with your own database, file storage solution, serverless function runtime (or FaaS provider) or otherwise use the preconfigured and integrated components by Graphcool. 

Graphcool itself is programming language agnostic. Functions can be implemented in any language that's supported by your function runtime. The API proxy layer effectively is a simple web server that can also be implemented in the language of your choice. You could thus use any of the afore mentioned frameworks to build the proxy layer.













