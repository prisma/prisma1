---
alias: zeez7aiph3
description: Using Hook Functions for data transformation & validation
---

# Validating and Transforming Mutation Input

In this article, you'll learn how to use serverless functions to validate an email address and make sure it gets saved in the database in lowercase letters.

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

The use case of sending a validating and transforming an email address is best implemented with hooks, so that's what we'll discuss in the remainder of this article.


## Getting Started with Hook Functions

A hook function consists of three elements that are specified in the `graphcool.yml` file:

1. The _operation_ the hook should be called for
2. Whether the hook should be called _before_ or _after_ the operation
3. The actual _function handler_ that will be executed

In the following, we'll walk through each step in detail with the goal of using a hook to validate and transform the email address of a new user.

### 0. Preparation

<Instruction>

We're going to use the [Graphcool CLI](https://www.npmjs.com/package/graphcool) to initialize our project.

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

### 1. Setup the Hook Function

Now we're adding the hook function to the service definition.

<Instruction>
Add a new entry to the `functions` list in `graphcool.yml` like so:

```yaml
functions:
  validateEmail:
    type: operationBefore
    operation: Customer.create
    handler:
      code: src/validateEmail.ts
```
</Instruction>


As mentioned above, this new hook function `validateEmail` consists of three parts:

* the `operationBefore` function type signifies that this is a hook function called _before_ the operation finishes
* the `Customer.create` operation signifies that this hook will be called when a customer is about to be created
* the `code` handler is used for managed Graphcool functions in contrast to `webhook` handlers

### 2. Write the Code

For hooks, it's important to note that we have an _input_ and an _output_. The _input_ is determined by the _operation_ that we chose, so in our case it's a JSON object that follows the structure of the `Customer`:

```graphql
type CustomerInput {
  id: ID!
  name: String!
  email: String!
}
```

The _output_ has to be another JSON object and has either one of two fields:

- `data`: When returning the `data` field we're communicating that the validation and transformation were successful and the data can "proceed".
- `error`: If the validation fails, we can return an `error` in the JSON object along with a string that represents the error message.

With that knowledge, let's go and implement our function.

<Instruction>
Create a new file `src/validateEmail.ts` like so:

```js
// 1. Import npm modules
import * as validator from 'validator'
import { FunctionEvent } from 'graphcool-lib'

// 2. Define type of event data
interface EventData {
  id: string
  name: string
  email: string
}

export default async (event: FunctionEvent<EventData>) => {
  // 3. Transform Email to lowercase
  event.data.email = event.data.email.toLowerCase()

  // 4. Validate Email
  if (!validator.isEmail(event.data.email)) {
    return { error: `${event.data.email} is not a valid email!` }
  }

  // 5. Return transformed data
  return { data: event.data }
}
```

</Instruction>

Let's try to understand the different parts of that function:

1. We're importing a Javascript module that we'll use to validate the email address. Additionally, we're importing `FunctionEvent` from `graphcool-lib` for its type information.
2. Here, we're defining the type of the input data. It's structured as mentioned above.
2. Now we're transforming the `email` address to be all lowercase.
3. Next we validate the email address and return a custom `error` if the validation fails.
5. If we got to this point, we simply return the `data` that now contains the lowercase email address.

Next, we'll make sure the required modules are installed.


<Instruction>
We need to install `validator` and `graphcool-lib`:

```sh
npm install --save validator graphcool-lib
```

This adds the dependencies to the `package.json` file.
</Instruction>

### 3. Deployment & Testing

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
       email: "NILAN@graph.cool"
       name: "Nilan"
     ) {
       email
     }
   }
   ```

## Conclusion

In this article you learned how to setup a [hook function](!alias-pa6guruhaf) using the Graphcool CLI. The function is called every time right before a new customer is created and ensures that the email address that was provided is actually valid and further transforms it to only have lowercase characters.
