# What is Graphcool

## A new kind of backend development framework

Graphcool is a backend development framework, similar to [Ruby on Rails](http://rubyonrails.org/), [Laravel](https://laravel.com/), [Django](https://www.djangoproject.com/) or [express.js](https://expressjs.com/). It combines the two most promising technologies in the API and infrastructure space today, [GraphQL](http://www.graphql.org/) and [Serverless](https://en.wikipedia.org/wiki/Serverless_computing), to provide a new level of abstraction for backend developers.

It's crucial to understand that this new level of abstraction also makes Graphcool different from previous frameworks. Rather than providing developers with *building blocks* that they pull into their projects and work with in the framework's programming language, Graphcool actually provides more of a “runtime” that can be extended with *functions* which can be written in any language - Graphcool thus is *programming language agnostic*! 

With Graphcool, it's possible to build truly event-driven applications where the different pieces of functionality are composed like Lego bricks!

## Traditional backend development is focussed on infrastructure

In traditional backend development, there are a couple of common infrastructure-related tasks that developers need to repeatedly perform to get an API up-and-running:

* Defining a data model and setting up the database
* Implementing an ORM to map from the database to the data structure exposed by the API
* Defining and building the API that provides access to the data
* Solutions for user authentication and file management
* Set everything up on scalable infrastructure
* ???

Looking closer at these tasks, they're all related to *infrastructure* but in-and-of-themselves don't provide any value to the end user. These tasks are repetitive and need to be performed in every new project. They're thus perfect candidates to be abstracted away by a framework - this is exactly what Graphcool does.

Graphcool provides developers with the right tools so they don't have to worry about infrastructure any more but instead can focus on custom business logic and value-adding features!

## GraphQL and Serverless enable a new architecture

As mentioned above, Graphcool is based on GraphQL and Serverless, two technologies that are each revolutionary approaches for the problem domains they've been developed for. Both technologies are still very young but are seeing major adoption in the developer communities. 

GraphQL is a query language for APIs, solving many of the problems and inefficiencies that developers experience when working with REST APIs. It is an open-source project that was started by Facebook and is now adopted and maintained by a large number of companies and individuals from all over the world.

Serverless on the other hand is a new paradigm that enables developers to write and deploy individual functions rather than having to take care of full servers that need to be implemented and managed. This lightweight approach to development lays the foundation for event-driven architectures and makes it easy to build large-scale applications that are composed from smaller units of functionality. 

Taken together, GraphQL and Serverless enable a completely new kind of architecture that's a game changer for server-side development. We're calling this: The Serverless GraphQL Backend Architecture. The idea of this architecture is heavily based on the principles mentioned in the [12 factor app methodology](https://12factor.net/).






