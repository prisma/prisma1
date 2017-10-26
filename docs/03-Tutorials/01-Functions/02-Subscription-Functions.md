---
alias: dee0aethoo
description: Using Subscriptions Function to initiate external workflows
---

# Sending a Welcome Email

In this article, you'll learn how to use serverless functions to send a welcome email to new users that are signing up with your app.

## Overview: Serverless Functions

[Serverless Functions](https://en.wikipedia.org/wiki/Serverless_computing), also referred to as Functions as a Service (FaaS), enable developers to write and deploy independent pieces of functionality without having to deal with setting up and configuring the server environment.

In that sense, they are similar to microservices, where each function represents one microservice, but again, without the developer having to deal with configuration or deployment overhead. Instead, they can take advantage of the server infrastructures of FaaS providers such as [AWS Lambda](https://serverless.com/framework/docs/providers/aws/guide/functions/), [Google Cloud Functions](https://cloud.google.com/functions/) or [StdLib](https://stdlib.com/).


## Graphcool Functions

When using Graphcool, you can use serverless functions to extend the functionality of your backend. A few common use cases are:

- _transforming_ data, e.g. removing the spaces from a credit card number
- _validating_ data, e.g. ensuring an email address is correct
- call _third-party services_ like Stripe or Mailgun

These are precisely the features that you can implement with serverless functions!

You generally have the choice between three different kinds of functions that serve slightly different purposes:

- [**Subscription Functions**](!alias-bboghez0go): Execute a serverless function _after_ a mutation happened in the backend
- [**Hook Functions**](!alias-pa6guruhaf): Allows for transformation and validation of mutation input and response payload at various stage
- [**Resolver Functions**](!alias-su6wu3yoo2): Extend your GraphQL API with additional queries and mutations that cover _any_ kind of functionality

The use case of sending a welcome email to new users is best implemented with a Subscription function, so that's what we'll discuss in the remainder of this article.


## Getting Started with Subscription Functions

A subscription function consists of two elements that are specified in the `graphcool.yml` file:

1. The _subscription query_ that determines when the subscription is triggered
2. The actual _function handler_ that will be executed

In the following, we'll walk through each step in detail with the goal of using the request pipeline to validate and transform the email address of a new user.

### 0. Preparation

<Instruction>

We're going to use the [Graphcool CLI](https://www.npmjs.com/package/graphcool) to initialize our project:

```sh
# Install the Graphcool CLI
npm install -g graphcool

# Create project
graphcool init
```

</Instruction>

Next, we'll add a new `Customer` model type to our service definition.

<Instruction>
Update the `types.graphql` file like so:

```graphql
type Customer @model {
  id: ID! @isUnique
  name: String!
  email: String!
}
```
</Instruction>

### 1. Setup the Subscription Function

Now we're adding the subscription function to the service definition.

<Instruction>
Add a new entry to the `functions` list in `graphcool.yml` like so:

```graphql
functions:
  sendWelcomeEmail:
    type: subscription
    query: src/welcomeEmail.graphql
    handler:
      code: src/welcomeEmail.js
```
</Instruction>

As mentioned above, this new subscription function `sendWelcomeEmail` consists of two parts:

* the `query` property refers to a `.graphql` file that contains the subscription query that determines when the subscription function is triggered
* the `code` handler is used for managed Graphcool functions in contrast to `webhook` handlers

### 2. Setup the Subscription Query

In this part, we need to write the subscription query into the file `src/welcomeEmail.graphql`. This part is identical to writing subscriptions on the frontend!

When writing the subscription query, we have to provide two major pieces of information:

- a _filter_ that imposes further restrictions on the event that triggers the subscription
- the _payload_ of the subscription that describes what data will be delivered when the subscription fires (note that this payload is the _input_ for the function that we're writing in the next step)

For the filter, we have two requirements. First, we only want to execute our function when new customers are created, so we want to ignore events where existing customers are updated or deleted. Second, since the `email` is an optional field on the `Customer` type, we want to make sure that it only fires for those `Customer`s that have an email address.

The payload on the other hand is pretty straightforward. Here we'll specify the `email` address of the customer and the `name` so that we can address them personally in the welcome email.

Putting this together, we end up with the following subscription query.

<Instruction>
Create a new file `src/welcomeEmail.graphql` with this content:

```graphql
subscription {
  Customer(filter: {
    mutation_in: [CREATED],
    node: {
      email_not: null
    }
  }) {
    node {
      name
      email
    }
  }
}
```
</Instruction>

### 3. Write the Code

<Instruction>
Create a new file `src/welcomeEmail.js`:

```js
// 1. Import npm modules (check: https://tehsis.github.io/webtaskio-canirequire/)
const fetch = require('isomorphic-fetch')
const FormData = require('form-data')

// 2. Mailgun data
const token = new Buffer(`api:key-__YOUR_MAILGUN_KEY__`).toString('base64')
const url = 'https://api.mailgun.net/v3/sandbox__YOUR_MAILGUN_SANDBOX__.mailgun.org/messages'

module.exports = function (event) {

  // 3. Extract info about new customer
  const { name, email } = event.data.Customer.node

  // 4. Prepare body of POST request
  const form = new FormData()
  form.append('from', 'Graphcool <hello@graph.cool>')
  form.append('to', `${name} <${email}>`)
  form.append('subject', 'Hello from Graphcool')
  form.append('text', `Welcome ${name}!`)

  // 5. Send request to Mailgun API
  return fetch(url, {
    headers: {
      'Authorization': `Basic ${token}`
    },
    method: 'POST',
    body: form
  })
}
```
</Instruction>

Let's try to understand the different parts of that function:

1. We're importing some Javascript modules that we need for our function.
2. Here we define constants that represent our personal Mailgun data.
3. We're extracting the info we need from the subscription event.
4. Now we put together and configure the `form` that represents our welcome email.
5. Finally we're using `fetch` to send the data as a `POST` request to Mailgun.

Next, we'll make sure the required modules are installed.

<Instruction>
We need to install `isomorphic-fetch` and `form-data`:

```sh
npm install --save isomorphic-fetch form-dat
```

This adds the dependencies to the `package.json` file.
</Instruction>

### 4. Deployment & Testing

Once you're done writing the function, you can deploy the changes to a new service:

```sh
graphcool deploy # select any shared-cluster
```

Afterwards, let's open the GraphQL Playground:

```sh
graphcool playground
```

We can use the Playground to send an actual mutation to your GraphQL API. A sample mutation could look as follows:

   ```graphql
   mutation {
     createCustomer(
       name: "John Doe",
       email: "john.doe@gmail.com"
      ) {
        id
      }
   }
   ```

## Conclusion

In this article you learned how to setup a [subscription function](!alias-bboghez0go) using the Graphcool CLI. The subscription fires every time a new `Customer` is created and triggers a _function_ that will send a welcome email to that new `Customer` using the Mailgun API.
