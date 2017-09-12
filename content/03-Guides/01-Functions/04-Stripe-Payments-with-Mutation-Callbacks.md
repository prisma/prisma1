---
alias: soiyaquah7
description: Use mutation callbacks to implement a custom Stripe payment workflow to verify credit card details and charge customers.
---

# Stripe Payment Workflow with Mutation Callbacks using micro and now

> Note: This guide uses the deprecated *mutation callbacks*. Use [Server-Side Subscriptions](!alias-ahlohd8ohn) instead.

In this guide, we'll explore implementing a custom Stripe payment workflow with Graphcool mutation callbacks. While you can use any serverless solution like AWS Lambda or Auth0 webtask, we'll use zeit's [micro](https://github.com/zeit/micro) and [now](https://zeit.co/now) in this tutorial.

You can find the code for this tutorial [here](https://github.com/graphcool-examples/micro-stripe-example).

Together we will go through these steps:

* register at Stripe and setup a Graphcool project for an example e-commerce application
* implement a process that creates a new Stripe customer whenever a Graphcool user adds new credit card details
* implement a process that charges the corresponding Stripe customer whenever a Graphcool user makes a new purchase
* test the two processes by creating example credit card details and purchases

## Register at Stripe

First, you need to register at [Stripe](https://stripe.com/) to obtain a **secret Stripe key** that allows you to add new Stripe customers and charge them. The secret key looks like this: `sk_test_XXXXXXXXXXXXXXXXXXXXXXXX`

> **Warning:** When following this guide, use the secret key associated with your **test account** to avoid charging real customers by accident. Once you are sure that all works as you intended, you can use your real Stripe account.

## Prepare your GraphQL schema

Next, we're preparing a Graphcool project for our example application. We're implementing a generic e-commerce platform where customers can sign up and order purchases. This is the corresponding GraphQL schema:

```idl
type User {
  id: ID!
  name: String!
  email: String!
  password: String!
  stripeId: String
  orders: [Purchase]
  cardDetails: CardDetails
}

type CardDetails {
  id: ID!
  cardToken: String!
  user: User
}

type Purchase {
  id: ID!
  description: String!
  amount: Int!
  isPaid: Boolean!
  user: User
}
```

Here is a checklist of necessary steps to end up with the correct schema:

* Add string fields `name`, `stripeId` to `User` - `stripeId` is not required!
* Create `CardDetails` type with string field `cardToken`
* Create `Purchase` type with the fields string `description`, int `amount`, boolean `isPaid`
* Create one-to-one relation `UserCardDetails`, `user` - `cardDetails`
* Create one-to-many relation `UserPurchases`, `user` - `purchases`
* Enable email/password provider

Additionally, make sure that `isPaid` for purchases has the default value `false`.

## Permission setup

These are the permissions that we use in your application:

* everyone can create a User node - meaning that everyone can sign up
* authenticated users can add card details to their own user node

> Note: Use a permission query on the createCardDetails mutation like this:

```graphql
{
  allUsers(filter: {
    AND: [{
      id: $userId
    }, {
      id: $new_userId
    }]
  }) {
    id
  }
}
```

* authenticated users can add purchases to their own user node

> Note: Use a permission query on the createPurchases mutation and make sure to unselect the `isPaid` field. This is the query:

```graphql
{
  allUsers(filter: {
    AND: [{
      id: $userId
    }, {
      id: $new_userId
    }]
  }) {
    id
  }
}
```

The default value `false` for `isPaid` and the missing permission to set `isPaid` when creating a new purchase guarantees that new purchases are automatically unpaid - ensuring that our payment workflow kicks in.

Additionally, we'll need to generate a [permanent authentication token](!alias-eip7ahqu5o#token-types) to give our microservices access to read and modify data in our GraphQL backend.

> Note: Make sure to remove all other permissions, especially all permissions for `CardDetails` and the `stripeId` field on `User` to ensure that no unauthorized actions can be done with the credit card credentials.

## Setup now

> Note: You can use any serverless solution like AWS Lambda or Auth0 Webtask, or even host your own microservices. In this tutorial though, we're continuing with Zeit's micro and now.

We'll setup two microservices and each will be used for a different mutation callback. First, let's install now:

```sh
npm install -g now
```

To protect our microservices from unauthorized access, let's generate two secret passphrases, we'll use length 32 here. Let's call them `create-secret` and `charge-secret`.

Now add the needed secrets to `now`:

* `now secret add stripe-secret sk_test_XXXXXXXXXXXXXXXXXXXXXXXX`
* `now secret add gc-pat XXX`
* `now secret add create-secret XXX`
* `now secret add charge-secret XXX`
* `now secret add endpoint https://api.graph.cool/simple/v1/__PROJECT_ID__`

### When new card details are created, create corresponding Stripe customer

Now we can add a new mutation callback with the trigger `CardDetails is created`. This mutation callback creates a new Stripe customer whenever new card details are created. Use this as its payload:

```graphql
{
  createdNode {
    id
    cardToken
    user {
      id
      email
      name
    }
  }
}
```

Now, when new card details are added to a specific user, we'll run this code by means of the mutation callback:

```js
stripe.customers.create({
    email: user.email,
    description: user.name,
    source: cardDetails.cardToken
  }, (err, customer) => {
    if (err) {
      console.log(err)
      send(res, 400, { error: `Stripe customer with card details ${cardDetailsId} could not be created for user ${userId}` })
    } else {
      // then update user with obtained Stripe customer id
      const updateUser = `mutation {
        updateUser(id: "${userId}", stripeId: "${customer.id}") {
          id
        }
      }`
      request.post({
        url: endpoint,
        headers: {
          'Authorization' : graphcoolToken,
          'content-type': 'application/json',
        },
        body: JSON.stringify({query: updateUser}),
      }).on('error', (e) => {
        send(res, 400, { error: `User ${userId} could not be updated` })
      }).on('response', (response) => {
        send(res, 200, { message: `User ${userId} was successfully registered at Stripe` })
      })
    }
  }
)
```

* First, we're calling `stripe.customers.create` to create a new Stripe customer using Stripe's JavaScript SDK.
* If the Stripe customer was created successfully, we store the Stripe customer id in the user node in our GraphQL backend using the `updateUser` mutation.
* We're calling the `updateUser` mutation using `request.post`. Note that we need to supply the permanent authentication token in the `Authorization` header to gain the needed permission access.

Deploy the microservices with now:

* `now -e STRIPE_SECRET=@stripe-secret -e GC_PAT=@gc-pat -e ENDPOINT=@endpoint TOKEN=@create-secret create/`

Now take the obtained url, add the `create-secret` as a query parameter and paste it to the mutation callback url. For example:

```
https://yourappname-create-customer-zstygsglsj.now.sh?token=XXX
```

Where `XXX` should be replaced with your `create-secret`.

### When purchase is created charge Stripe customer

We'll add another mutation callback that handles the actual charging when a new `Purchase is created`. Use this payload:

```graphql
{
  createdNode {
    id
    user {
      stripeId
    }
    amount
    description
    isPaid
  }
}
```

When a new purchase node is created, we'll run the following code. First, we check if the purchase has already been paid to avoid charging twice:

```js
if (purchase.isPaid) {
  send(res, 400, { error: `Customer ${customerId} could not be charged, because purchase ${purchaseId} was already paid` })
}
```

If the purchase has not been paid yet, we'll continue the charging process:

```js
stripe.charges.create({
  amount: purchase.amount,
  currency: 'usd',
  description: purchase.description,
  customer: customerId,
}, (err, charge) => {
  if (err) {
    console.log(err)
    send(res, 400, { error: `Customer ${customerId} could not be charged` })
  } else {
    const mutation = `mutation {
      updatePurchase(id: "${purchaseId}", isPaid: true) {
        id
      }
    }`

    request.post({
      url: endpoint,
      headers: {
        'Authorization' : graphcoolToken,
        'content-type': 'application/json',
      },
      body: JSON.stringify({query: mutation}),
    }).on('error', (e) => {
      send(res, 400, { error: `Customer ${customerId} was charged, but purchase ${purchaseId} was not marked as paid` })
    }).on('response', (response) => {
      send(res, 200, { message: `Customer ${customerId} was charged and purchase ${purchaseId} was marked as paid` })
    })
  }
})
```

* First, we're calling `stripe.charges.create` to create a new Stripe charge using Stripe's JavaScript SDK.
* If the Stripe charge was created successfully, we're marking the corresponding purchase as paid.
* We're calling the `updatePurchase` mutation using `request.post`. Note that we need to supply the permanent authentication token in the `Authorization` header again.

Deploy the microservices with now:

* `now -e STRIPE_SECRET=@stripe-secret -e GC_PAT=@gc-pat -e ENDPOINT=@endpoint TOKEN=@charge-secret charge/`

Now take the obtained url, add the `charge-secret` as a query parameter and paste it to the mutation callback url. For example:

```
https://yourappname-charge-customer-asyygagwsj.now.sh?token=XXX
```

Where `XXX` should be replaced with your `charge-secret`.

## Test the Stripe Payment Workflow

### Create a user

We'll create a first user that we'll use to make test purchases:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixysf7tr0id50173u64kn6zi
disabled: true
---
mutation {
  createUser(
    name: "Nilan"
    authProvider: {
      email: {
      	email: "nilan@graph.cool"
        password: "password"
      }
    }
  ) {
    id
  }
}
---
{
  "data": {
    "createUser": {
      "id": "cixyw53tg6i8a0173kx2nrwto"
    }
  }
}
```

### Create card details with a Stripe card token

Now in your frontend application, you need to use one of Stripe's integrations to obtain a valid credit card token. Using Stripe.js, this code creates a valid token for a test credit card:

```js
Stripe.card.createToken({
  number: '4242424242424242',
  exp_month: 12,
  exp_year: 2018,
  cvc: '123'
}, function(status, response) {
  // response.id is the card token. now we can call the mutation
  // createCardDetails(cardToken: "response.id", userId: "userId")
  // to add new card details for the signed in user
  console.log(response.id)
})
```

Alternatively, you can generate a valid token for a test credit card to your Stripe account in the [Stripe documentation](https://stripe.com/docs#try-now).

Using the token we can now create new card details for our test user:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixysf7tr0id50173u64kn6zi
disabled: true
---
mutation {
  createCardDetails(
    cardToken: "tok_19cIRbD7TaYA8JWHv5WIEOiL"
    userId: "cixyw53tg6i8a0173kx2nrwto"
  ) {
    id
  }
}
---
{
  "data": {
    "createCardDetails": {
      "id": "ciyjwspy33sod0127v904gjtu"
    }
  }
}
```

Now our first microservice should kick in and create a new Stripe customer corresponding to the card and user details. Go to your Stripe account to confirm.

### Make a purchase

Now whenever the test user makes a purchase, the second microservice makes sure that the corresponding Stripe customer we'll be charged. To confirm this, create a new test purchase in the Graphcool playground:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixysf7tr0id50173u64kn6zi
disabled: true
---
mutation {
  createPurchase(
    amount: 50000,
    description: "A new laptop",
    userId: "cixyw53tg6i8a0173kx2nrwto"
  ) {
    isPaid
  }
}
---
{
  "data": {
    "createPurchase": {
      "isPaid": false
    }
  }
}
```

The mutation callback charges the corresponding Stripe customer and after a few seconds, `isPaid` is set to `true` to signify that the amount has been charged. Confirm that in both your Stripe account (a new charge should appear) and in your Graphcool data (`isPaid` should be set to true).

> Note: Stripe uses integers to express the amount of money charged. In this case, a total of 1000 equals 10 USD.

## Next steps

The mutation callback system as demonstrated is flexible enough to not only handle one-off purchases, but also Stripe Subscriptions for recurring payments, Stripe Connect for marketplace applications and more! You can find out more about the available options in the [Stripe documentation](https://stripe.com/docs).
