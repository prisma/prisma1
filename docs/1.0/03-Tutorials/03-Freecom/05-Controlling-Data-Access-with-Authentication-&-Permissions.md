---
alias: pei9aid6ei
description: Controlling Data Access with Authentication & Permissions
---

# Freecom Tutorial: Controlling Data Access with Authentication & Permissions (4/6)

<InfoBox type=warning>

**Note**: This guide is only applicable to [legacy Console project](!alias-aemieb1aev). It doesn't work with the new [Graphcool Framework](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) which is based on the [`graphcool.yml`](!alias-foatho8aip) service definition file.

An updated version of this guide is coming soon, stay tuned!

</InfoBox>

In this fourth chapter of our [Freecom tutorial series](!alias-e8a6ajt8ax) you will learn how to use the autentication and permission features of Graphcool to control data access of your users. The [last chapter](!alias-die6mewitu) was about realtime functionality using GraphQL subscriptions. The goal for today is to make sure that customers only have access to their own conversations.

<iframe height="315" src="https://www.youtube.com/embed/RHI1affZAvM" frameborder="0" allowfullscreen></iframe>


## Authentication in Freecom

In most applications, authentication requires the user to create an account by providing email and password or by using a _social login_ as provided by Google, Facebook or GitHub, etc.

While that's a valid scenario for most applications, it doesn't quite fit the use case of Freecom since users should be able to start chatting with support agents right away and without having to go through a login process. However, we also want to make sure that customers only have access to messages in their own conversations. How can we still authenticate users and specify permissions and access rights?


## Auth Providers

This dilemma can be solved using Graphcool's [Anonymous Auth Provider](!alias-wejileech9). In general, Graphcool implements authentication functionality based on so-called [Auth Providers](!alias-wejileech9/#auth-providers). These Auth Providers will generate tokens that are used to authenticate requests against the API and check the data access permissions of the user who sent the request. If the token is associated with a user that doesn't have the required permissions, an error will be returned.


## Enabling Anonymous Authentication

To use the anonymous auth provider in our app, we first need to enable it in the **Integrations** section in the [Graphcool console](https://console.graph.cool):

![](./fc4-anon-auth-provider-1.png?width=650)

In the popup, we have to specify the _type_ that we'd like to be able to authenticate - in our case that's the `Customer`:

![](./fc4-anon-auth-provider-2.png?width=500)

Once the integration is enabled, there is a new mutation available that's looks as follows:

```graphql
authenticateAnonymousCustomer(secret: String!): token
```

This mutation can be used to create a new `Customer` by providing a _secret_. The secret should be unique and not publicly available, since it can be used to generate a new session for the customer it belongs to.


## Authentication with Apollo Client

As mentioned above, the `authenticateAnonymousCustomer` mutation returns an authentication token that we have to put into the `Authorization` header field on subsequent API requests. This token will identify and authenticate the corresponding customer on the server-side.


### Creating the Mutation

To get access to the authentication token, we first need to send the mutation whenever we want to create a new `Customer` in the backend. We'll therefore replace the mutation we previously used to create new customers with the following one:

```js
const createAuthenticatedCustomer = gql`
  mutation authenticateAnonymousCustomer($secret: String!) {
    authenticateAnonymousCustomer(secret: $secret) {
      id
      token
    }
  }
`
```

We're including the `token` as well as the `id` of the newly created `Customer` in the payload of the mutation so that we can save it in _local storage_ and access it later on.

As usual, we need to combine our React component with that mutation using Apollo's [`graphql`](http://dev.apollodata.com/react/api-graphql.html) higher-order component to make the mutation available in the component's props:

```js
graphql(initializeCustomer, {name: 'createAuthenticatedCustomerMutation'})(App)
```

The `secret` that needs to be passed to the mutation is generated using [cuid](https://github.com/ericelliott/cuid). Here's what performing the mutation looks like:

```js
const secret = cuid()
const authenticationResult = await this.props.createAuthenticatedCustomerMutation({
  variables: { secret }
})
const authToken = authenticationResult.data.authenticateAnonymousAuthenticated.token
const customerId = authenticationResult.data.authenticateAnonymousAuthenticated.id
localStorage.setItem(AUTH_TOKEN_KEY, authToken)
localStorage.setItem(FREECOM_CUSTOMER_ID_KEY, customerId)
```

Whenever this code is executed, a new `Customer` will be created in the database and their corresponding authentication token is stored in local storage.

### Configuring Apollo Client

We need to put this token into the header of the HTTP request, more precisely into its `Authorization` field. However, since we're not performing any network requests directly when we're using Apollo, we have no direct access to the requests' headers. Apollo thus gives us the option to configure our instance of the `ApolloClient` such that it attaches the token for us with every request.

Apollo Client uses the concept of [_middleware_](http://dev.apollodata.com/core/network.html#networkInterfaceMiddleware) for this. It's possible to add an additional step to the process of sending a network request by adding a middleware to the `networkInface` that we're passing to the `ApolloClient` constructor:

```js
networkInterface.use([{
  applyMiddleware(req, next) {
    if (!req.options.headers) {
      req.options.headers = {}
    }

    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    req.options.headers.authorization = token ? `Bearer ${token}` : null
    next()
  }
}])
```

## Controlling Data Access with Permission Queries

We now have our frontend setup to authenticate new customers. However, from a functionality perspective nothing has actually changed, the app will continue working in the same way that it did before. That's why we also need to configure the access control and permissions in the backend to actually restrict data access and make sure the authentication token actually has a purpose.

To recap, our requirement is that only the customer that is part of a specific `Conversation` should have access to the messages that are associated with it. This includes:

- Reading messages from that conversation
- Creating new messages in that conversation
- Modifying and deleting existing messages in that conversation


### Permissions in Graphcool

Graphcool follows a [_whitelist_](!alias-iegoo0heez/#whitelist-permissions-for-modular-authorization) approach when it comes to data access. That means that by default, no operation is permitted. The only way to read or update data is by explicitly allowing (i.e. _whitelisting_) the respective operation.

However, note that when you're creating new types for your schema, a set of permissions is generated for you allowing _all operations_ - this is why we were able to interact with the API in the previous chapter even though we've never actually created any permissions. You can verify this by selecting the **Permissions**-tab in the [Graphcool console](https://console.graph.cool).

![](./fc4-permissions-overview.png?width=650)

When restricting the access for specific operations on a type, we have two options:

1. Add the general requirement that the user performing the operation needs to be _authenticated_
2. Exercise fine-grained control and declare _specific requirements_ which the user has to meet in order to be allowed to perform the operation, using a so-called [permission query](!alias-iox3aqu0ee)

![](./fc4-permissions-popup.png?width=500)

If we only specify the first option, a user that sends a valid authentication token in the request's header will be able to perform the corresponding operation.

_Permission queries_ on the other hand are more sophisticated and allow to express data access rules by leveraging the power of GraphQL queries.


### Writing a Permission Query

To implement our requirements from above, we have to take the second approach and actually write a permission query to specify that customers can only access messages in their own conversations.

Since we want to restrict access on the `Message` type, we are going to adjust the four already existing permissions for _reading_, _creating_, _updating_ and _deleting_ on it. Right now, all operations are allowed for all users:

![](./fc4-message-permissions.png?width=350)

Let's adjust the permission for the _Read_ operation. Simply click the corresponding row to bring up the configuration popup.

In the popup, we first have to specify which _fields_ this permission should apply to. At this point we could potentially specify that _everyone_ should still be able to read the `id` of the messages, but only a specific audience can read the `text` and other fields. However, we actually want to restrict the access on the whole type, so we're going to select all the fields:

![](./fc4-select-fields.png?width=500)

Next, we are switching to the **Define Rules** tab of the popup to specify the permission query.

There are a few things we need to know about permission queries before we start writing one:

- A permission query always returns `true` or `false`.
- The permission query will be executed right before the operation is performed in the backend.
- Only if it returns `true`, the operation will actually be performed.
- The `$node_id` that's passed in identifies the node on which the operation is to be performed, in our case that'll be the `Message` to be read.
- If we require authentication for an operation (by ticking the **Authentication required** checkbox), we also have access to the ID of the user who wants to perform the operation as an argument inside the query. This allows to specify the permission with respect to the currently authenticated user!

The last point particularly is important, since we indeed want to express a permission requirement where the current user is involved. We thus first have to check the **Authentication required** checkbox before starting to write the query.

Once the box is checked, we can write the query like so:

```graphql
query ($user_id: ID!, $node_id: ID!) {
  SomeMessageExists(
    filter: {
      id: $node_id,
      conversation: {
        customer: {
          id: $user_id
        }
      }
    }
  )
}
```

Let's try to understand exactly what's going on by considering an example. Let's say we have the following message in our database:

```js
{
  "id": "cj043azl97gt00140myd46nec",
  "text": "Hey there 😎",
  "conversation": {
    "customer": {
      "id": "cj043aw1b6ts10143nyd5xzhh",
      "name": "Violetspear-Puma"
    }
  },
  "updatedAt": "2017-03-10T17:22:14.000Z",
  "createdAt": "2017-03-10T17:22:14.000Z"
}
```

Now the user `Violetspear-Puma`, who is indeed the sender of the message, tries to read it with the following query (assuming a valid authentication token is attached to the request header):

```graphql
{
  Message(id: "cj043azl97gt00140myd46nec") {
    id
    text
  }
}
```

Before that query is executed, our permission query from above will be performed with the following arguments:

- `$user_id`: the `id` of `Violetspear-Puma`: `cj043aw1b6ts10143nyd5xzhh`
- `$node_id`: the `id` of the message to be read: `cj043azl97gt00140myd46nec`

The `SomeMessageExists` query checks if a node of type `Message` exists such that the filter conditions are true.

The requirements in the `filter` are:

- a node of type `Message` with the `id` equal to `$node_id` (which we saw is `cj043azl97gt00140myd46nec`)
- a node of type `Message` that is connected to a `conversation` which again is connected to a `customer` where the `id` of that `customer` is equal to `$user_id` (which we saw is `cj043aw1b6ts10143nyd5xzhh`)

Since both conditions are true with our sample message from above, the query will indeed return `true`, thus allowing the user to perform the read operation and return the specified information.

The permissions queries for the remaining operations on the `Message` type (_Create_, _Update_ and _Delete_) look identical to the one from the _Read_ operation.


## Wrap Up

That's it for today's Freecom chapter! We learned how to express precise data access permissions for our users. For the Freecom app, we restricted the access on the `Message` type only to customers who are directly involved in the conversation where this message appears.

Authentication in the Graphcool backend was done using the _anonymous auth provider_ which allows users to authenticate without having to leave their email address. When using this approach, the server returns an authentication token that is sent along with all subsequent requests. This token is then used to identify the user on the server-side and enabling to check the access permissions of the user performing the request.

In the next chapter, we'll enable support agents to respond to a customer by integrating Slack using _mutation callbacks_ and _serverless functions_.

Let us know how you like the tutorial or ask any questions you might have. Contact us on [Twitter](https://twitter.com/graphcool) or join our growing community on [Slack](http://slack.graph.cool/)!

<!-- FREECOM_SIGNUP -->
