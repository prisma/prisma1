# Full Example: Webshop :moneybag:

## What it includes

- GraphQL type definitions
- Stripe Checkout Flow
- A GraphQL `resolver` function
- small seed script

## Config

This example needs an environment variable called `STRIPE_TEST_KEY`.
The `STRIPE_TEST_KEY` can be obtained by [creating a stripe account](https://dashboard.stripe.com/register) and getting the token from the Account Settings.

## Setup

Download the example or [clone the repo](https://github.com/graphcool/graphcool):

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/full-example
cd full-example
```

Install the CLI (if you haven't already):

```sh
npm install -g graphcool
```

You can now [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. Before that, you need to install the node [dependencies](package.json#L14) for the defined functions:

```sh
yarn install      # install dependencies - alternatively npm install
graphcool deploy  # deploy service
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`.

Copy the _Simple API endpoint_ for the next step.

## Data Setup

You can run the seed script to get started. First, obtain the _root token_ needed for the script:

```sh
graphcool root-token seed-script
```

Replace `__ENDPOINT__` and `__ROOT_TOKEN__` with the two copied values, and run the script to create a few product items:

```sh
node src/scripts/seed.js
```

## Basic Workflow

* signup as a new user
* login by setting authorization header
* create a new order
* add a couple of items
* pay the order

### Authentication

Signup as a new user and copy both the `id` and `token` for later use.

```graphql
mutation signup {
  signupUser(
    email: "nilan@graph.cool"
    password: "password"
  ) {
    id
    token
  }
}
```

If you want to login again at a later point, you can use this mutation:

```graphql
mutation login {
  authenticateUser(
    email: "nilan@graph.cool"
    password: "password"
  ) {
    token
  }
}
```

When you set the `Authorization` to `Bearer <token>`, your requests are authenticated. You can use this query to obtain the authenticated user id:

```graphql
query loggedInUser {
  loggedInUser {
    id
  }
}
```

### Checkout Process

First, we'll create a new order for the current user. Set the id of the user you created as the `$userId: ID!` variable and run this mutation:

```graphql
mutation beginCheckout($userId: ID!) {
  createOrder(
    description: "Christmas Presents 2018"
    userId: $userId
  ) {
    id
  }
}
```

This will create a new order with order status `NEW` (note the `orderStatus: OrderStatus! @defaultValue(value: NEW)` field in `types.graphql`). Copy the obtained `id` of the order.

Now, we can add as many items as we want to the order. Make sure you've run the seed script from above, then first query existing products:

```graphql
query existingProducts {
  allProducts {
    id
    price
    name
  }
}
```

For any product you'd like to add, copy its `id` and specify an amount:

```graphql
mutation addOrderItem($orderId: ID!, $productId: ID!, amount: Int!) {
  setOrderItem(
    orderId: $orderId
    productId: $productId
    amount: $amount
  ) {
    itemId
    amount
  }
}
```

You can run `setOrderItem` multiple times to change the amount of a specific product, or to remove it from your order entirely.
Once you're happy with the product selection in your order, you can pay (you'll need a valid Stripe token):

```graphql
mutation pay($orderId: ID!, $stripeToken: String!) {
  pay(
    orderId: $orderId
    stripeToken: $stripeToken
  ) {
    success
  }
}
```

To get all orders of a specific user, you can use this query:

```graphql
query orders($userId: ID!) {
  allOrders(filter: {
    user: {
      id: $userId
    }
  }) {
    description
    orderStatus
    items {
      amount
      product {
        name
      }
    }
  }
}
```
