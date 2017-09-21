---
alias: iph5dieph9
description: Frequently asked questions about basic topics and general issues of the Graphcool platform.
---

# Basics

### Is Graphcool a web framework, similar to Ruby on Rails or Express.js?

Graphcool is a backend development framework. In that sense it can be compared to frameworks like Ruby on Rails, Epress.js or Laravel (PHP), because it enables you to easily build backends. 

However, Graphcool is different from the above because it is _language-agnostic_. In contrast to all the above frameworks, you're not writing your actual web server in a specific language. Instead, you're specifying a data model using the GraphQL SDL. Based on this data model, Graphcool will generate a GraphQL API for you that provides CRUD operations for every model type.

Business logic and any other kind of functionality is then implemented using serverless functions or with webhooks, which can provides lots of flexibility and effectively can be implemented in almost any programming language!


### What programming languages can I use Graphcool with?

Graphcool uses the GraphQL SDL to generate the core GraphQL API for you that contains CRUD operations for your data model. Business logic and any other kind of functionality is then implemented using serverless functions or with webhooks.

When using Graphcool _managed functions_, you can only use Javascript at the moment. It is however planned to enable support of mutliple FaaS providers, and along with it various other programming languages.

> Managed functions are functions that will be deployed in the Graphcool function environment so you don't have to choose your own FaaS provider and manage and deploy the functions yourself.

If you're using webhooks to implement your business logic and other functionality, you're free to choose whatever technology stack and programming language you like as long as it can be invoked via HTTP.


### How does Graphcool compare to Firebase or other Backends-as-a-Service (BaaS)?

Graphcool is a _backend development framework_ and as such a lot more powerful and flexible than conventional BaaS. On the other hand, the _hosted version_ of Graphcool comes close to what's commonly understood under the term BaaS and offers the same level of convenience for frontend and mobile developers! 

The main differences between Graphcool and conventional BaaS are the following:

- You can host Graphcool yourself
- Graphcool makes use of GraphQL instead of REST (or other proprietary ways to access your data using e.g. an SDK like Firebase)
- When using the hosted version of Graphcool, there is first-class support for implementing custom business logic
- The permission system of Graphcool is a lot more expressive and powerful than the ones of other BaaS since it lets you define your permission rules with GraphQL queries that can access your whole data graph

The main commonalities with conventional BaaS and the hosted version of Graphcool are the following:

- Convenience of an auto-generated, hosted API and a managed database
- Built-in authentication mechanisms
- Rapidly creating your production-ready backend



### What is the difference between the hosted and local (self-hosted) version of Graphcool?

When using Graphcool, you can choose either of two versions:

- The _hosted version_ gives you much of the convenience that you also get from conventional BaaS.
- The _self-hosted version_ allows you to run Graphcool in your own server evnrinment.

When choosing the _self-hosted version_, you need to take care of the backend infrastructure and deployment (using Docker) yourself. In the future, it is planned to make Graphcool more flexible so you can also plug-in your own database - right now only AWS Aurora and MySQL are supported.

















