# Graphcool Webshop :moneybag:

## What it includes
- Anonymous + Email Authentication
- Stripe Checkout Flow
- GraphQL type definitions
- A custom GraphQL `resolver` function, hosted in Graphcool
- Permissions with Permission Queries for all Types (`User`, `Cart`, `Order`)

## Config
This example needs an environment variable called `STRIPE_KEY`.
The `STRIPE_KEY` can be obtained by [creating a stripe account](https://dashboard.stripe.com/register)
and getting the token from the Account Settings.

## Getting Started

```sh
$ npm install -g graphcool@beta
$ git clone git@github.com:graphcool-examples/graphcool-examples.git
$ cd graphcool-examples/webshop
$ graphcool init
```

## Data Setup

You can open the playground with `graphcool playground` and execute the following mutation to set up data.
```graphql
mutation init {
  createUser(
    firstName: "Bob"
    lastName: "Meyer"
    email: "bob.meyer@test.com"
    address: "Secret Address"
    baskets: [{
      items: [{
        name: "iPhone X"
        price: 1200
        imageUrl: "https://cdn.vox-cdn.com/uploads/chorus_image/image/56645405/iphone_x_gallery1_2017.0.jpeg"
        description: "The new shiny iPhone"
      }]
    }]
  ) {
    id
    baskets {
      id
    }
  }
}
```

## Application Flow
 1. User is logged in or creates anonymous Account
 2. User creates a `Cart` that he puts `Item`s into
 3. When the User wants to Checkout, he insert his credit cart and address. We can use the stripe docs to mimic this step:
    Obtain a Stripe token by using the **Try Now** example in their docs: https://stripe.com/docs
 4. With the inserted Data we're also updating the `User` we created in the beginning, adding the name and address of the person
 5. Create a new Order in Graphcool with the Stripe Token, the `userId` and `basketId` you just created:
 ```graphql

mutation order {
  createOrder(userId: "cj7kn28nn6fa90117qjwnut9v" stripeToken: "tok_ybnh1HWnDZKMonE6lVkHLMVt" basketId: "cj7kn28no6faa01175c8rsgsd") {
    id
  }
}
 ```
 3. Pay the Order that you just created by calling the custom resolver:
 ```graphql

mutation pay {
  pay(orderId: "cj7knpozv7t6f01535quuud9s") {
    success
  }
}
 ```

## Local Development
To run the `pay.js` function locally, you can use the `scripts/run.js` file to run it.

## IDE Support
The `graphql.config.json` file points to an endpoint hosting the Graphcool definition schema, which enables schema validation
on your GraphQL types definitions.

## Coming Soon
- Custom Permissions
- Schema Stitching
