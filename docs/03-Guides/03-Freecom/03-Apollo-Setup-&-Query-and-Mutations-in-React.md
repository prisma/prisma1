---
alias: oe8ahyo2ei
description: Learn how to integrate Apollo into your React app and run GraphQL queries & mutations.  
---

# Freecom Tutorial: Apollo Setup & GraphQL Queries/Mutations in React (2/6)

<InfoBox type=warning>

**Note**: This guide is only applicable to [legacy Console project](!alias-aemieb1aev). It doesn't work with the new [Graphcool Framework](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) which is based on the [`graphcool.yml`](!alias-foatho8aip) service definition file.

An updated version of this guide is coming soon, stay tuned!

</InfoBox>

This is the second chapter of our tutorial series where we explain how to build an Intercom clone using a Graphcool backend. In the [last chapter](!alias-xuakjj68lp), we already prepared the GraphQL server and can now start interacting with the API from our React application! 🚀

<iframe height="315" src="https://www.youtube.com/embed/ZItsQWNPw1U" frameborder="0" allowfullscreen></iframe>


## What is a GraphQL Client?

When working with GraphQL APIs, client applications will have to perform a lot of repetitive tasks. Some of these tasks might be relatively trivial, such as sending a GraphQL query in the body of an HTTP POST request or parsing the JSON response that is received from the server. Other tasks like caching data or obtaining realtime updates using GraphQL subscriptions are a lot more complicated to implement but still are desirable in lots of applications.

This is why it's beneficial to use GraphQL client libraries that will perform these tasks for you. At the moment, there are two popular GraphQL clients that differ very much in their complexity: [**Apollo**](http://dev.apollodata.com/) & [**Relay**](https://facebook.github.io/relay/).

## How to choose the right GraphQL Client?

When it comes to comparing Apollo and Relay, it really depends on the kind of application you're planning to build. Relay is highly optimized for performance, but it's also quite opinionated, includes a lot of _magic_ and generally won't give you a lot of flexibility. It also comes with a notable learning curve, so make sure you understand that your application can really benefit from Relay's features before choosing it. Apollo on the other hand provides lots of flexibility, is platform independent and implements a way to use [GraphQL subscriptions](https://dev-blog.apollodata.com/graphql-subscriptions-in-apollo-client-9a2457f015fb) which is an important factor to take into consideration if you want to have realtime functionality in your app!

We also published an [extensive comparison between Apollo and Relay](https://www.graph.cool/docs/tutorials/relay-vs-apollo-iechu0shia/) to help you pick the right tool for the job.


## Why Apollo Client?

[Apollo Client](http://dev.apollodata.com/) is a flexible GraphQL client implementing various features that enable you to take full advantage of a GraphQL API:

- easily send **queries and mutations**
- **caching** data of previous queries
- **optimistic UI** to reflect changes without waiting on server responses
- **realtime updates** through subscriptions
- out-of-the-box **server-side rendering**
- **pagination** to easily work with long lists of data

You can use Apollo in combination with any frontend technology and there are bindings available to use it with [React](http://dev.apollodata.com/react/) and [Angular](http://dev.apollodata.com/angular/). Apollo itself however is platform agnostic and can also be used from [plain Javascript](http://dev.apollodata.com/core/).

Apollo also shows great efforts in bringing their GraphQL client to native mobile platforms. At the time of writing this tutorial, the [iOS](http://dev.apollodata.com/ios/) client is already in a quite advanced state and an [Android](http://dev.apollodata.com/android/) client is in the making as well.

If you want to try out and learn about Apollo Client separately, make sure to check out our interactive tutorial on [**Learn Apollo**](https://learnapollo.com).


## Setting up Apollo Client in React

> **Tutorial Workflow**
>
> We're hosting all the code for the tutorial on [GitHub](https://github.com/graphcool-examples/freecom-tutorial). Each chapter comes with _two_ folders:
> 1. `freecom-0X`:  Contains the starter code for chapter `X`
> 2. `freecom-0X-final`: Contains the final code for chapter `X` and serves as a reference solution if you get lost along the way
>
> Each written chapter gives a high-level overview on the current topic. For step-by-step instructions watch the corresponding video.

As mentioned before, Apollo provides the [react-apollo](https://github.com/apollographql/react-apollo) package to easily get started with React as a frontend technology.

In the following, we'll explain the basic setup for Apollo in a React application. You can read more about all setup options in the Apollo [docs](http://dev.apollodata.com/react/initialization.html).


### 1. Including the Dependencies

If you're using `npm`, you can use the following command to add the `react-apollo` dependency that you need to get started:

```sh
npm install react-apollo --save
```

### 2. Instantiating the `ApolloClient`

Before you can make use of any of the functionality implemented in Apollo, you need to instantiate the [`ApolloClient`](http://dev.apollodata.com/core/apollo-client-api.html#apollo-client) and integrate it in your application. This `ApolloClient` instance will do most of the GraphQL-related work for you and basically serves as your interface to the GraphQL API.

When instantiating it, you'll have to provide a [`NetworkInterface`](http://dev.apollodata.com/core/apollo-client-api.html#NetworkInterface) so that the client knows which GraphQL endpoint it should connect to.

The code for creating the `NetworkInterface` along with the `ApolloClient` looks as follows:

```js
import { ApolloClient, createNetworkInterface } from 'react-apollo'

const client = new ApolloClient({
  networkInterface: createNetworkInterface({ uri: 'https://api.graph.cool/simple/v1/__YOUR_PROJECT_ID__' }),
})
```

> Note: You can find your project Id in our [console](https://console.graph.cool). Simply select your project in the left sidebar and then navigate to `Settings -> General`.


### 3. Instantiating the `ApolloProvider`

The `ApolloProvider` implements the bindings from Apollo to React. It's a component that, when wrapped around other React components, allows these children to interact with the specified GraphQL endpoint.

![](./fc2-architecture-overview.png?width=600)

In the picture, the React application is wrapped with the `ApolloProvider`, which again is associated with the `ApolloClient` that knows how to connect to the GraphQL API through its `NetworkInterface`. In code, the integration of the `ApolloProvider` looks as follows:

```js
import ApolloClient, { createNetworkInterface, ApolloProvider } from 'react-apollo'

const client = new ApolloClient({
  networkInterface: createNetworkInterface({ uri: 'https://api.graph.cool/simple/v1/__YOUR_PROJECT_ID__' }),
})

ReactDOM.render(
  <ApolloProvider client={client}>
    <App />
  </ApolloProvider>
  ,
  element
)
```

In this case, `App` is the root component of our application. Since we're wrapping it with the `ApolloProvider`, we enable every component in the entire component hierarchy to potentially interact with the GraphQL backend through Apollo.


## Sending Queries and Mutations with Apollo

You generally have two options when it comes to sending GraphQL queries and mutations with Apollo:

1. Wrapping a component with the `graphql` higher-order component
2. Directly calling `query` or `mutate` on the `ApolloClient` instance

### Option 1: Wrap the Component with `graphql`

The first option is to wrap the component using `graphql` which is a function that takes in a GraphQL query or mutation as well as a regular React component. The nice thing about this approach is that the response data of the query or mutation will automatically be available through the props of the component, so you don't have to implement any kind of callback to handle the response.

This is generally what you want if a component is supposed to display the data that is received from a query.

Let's see what the code would look like if we wanted our `Chat` component to query all the messages from a specific conversation while also giving it the ability to perform a `createMessage` mutation:


```js
import { graphql, compose, gql } from 'react-apollo'

const createMessage = gql`
  mutation createMessage($text: String!, $conversationId: ID!) {
    createMessage(text: $text, conversationId: $conversationId) {
      id
      text
    }
  }
`

const allMessages = gql`
  query allMessages($conversationId: ID!) {
    allMessages(filter: {
      conversation: {
        id: $conversationId
      }
    }) {
      id
      text
    }
  }
`

class Chat extends Component {

  render() {

    // return loading indicator if the data is still loading
    if (this.props.allMessagesQuery.loading) {
      return <div>Loading</div>
    }

    // retrieve the messages from the query
    const messages = this.props.allMessagesQuery.allMessages

    // use `messages` to render the component
    return ...
  }

  _onSend = () => {
    this.props.createMessageMutation({
      variables: {
        text: 'Hello',
        conversationId: this.props.conversationId
      }      
    }).then(response => {
      // handle the response
    })
  }

}

export default compose(
  graphql(allMessages, {name: 'allMessagesQuery'}),
  graphql(createMessage, {name : 'createMessageMutation'})
)(Chat)
```

Notice that this time, the `allMessagesQuery` also takes in an argument which is the `conversationId` of the `Conversation` that we're filtering for, so that we only retrieve messages that belong to this specific conversation.

However, since we're not explicitly sending the query using the `ApolloClient`, we don't have a way to pass the required argument `conversationId` to some function call. In such a case when we're using `graphql` to wrap our component with a query, we need to provide the input arguments for the query (or mutation) through the props of the component!

So, in our case, the `allMessagesQuery` will only work if the `Chat` component's props contain a value for the key `conversationId`.


### Option 2: Directly use the `ApolloClient`

The second option is to use the `query`/`mutate`-methods directly on the `ApolloClient` instance and handle the response with a [_promise_](https://developers.google.com/web/fundamentals/getting-started/primers/promises). For this approach, you need to explicitly make the `ApolloClient` instance available to the React component that should send the request by wrapping the component using [`withApollo`](http://dev.apollodata.com/react/higher-order-components.html#withApollo).

This option can be handy when you need to send a query or mutation only once, for example when you need a single bit of information upon initialization of a component, but it's not important that this information is available later on.

A simple query that fetches all the messages from the database could then be sent as follows:

```js
import { withApollo, gql } from 'react-apollo'

class Chat extends Component {

  componentDidMount() {
    // this requires wrapping `Chat` with `withApollo`
    this.props.client.query({
      query: gql`
        query allMessages {
          allMessages {
            id
            text
          }
        }
      `
    }).then(response => {
      // handle the response
    })
  }

  // ...

}

export default withApollo(Chat)
```

The syntax for a mutation is identical, except that you're calling `mutate` on the `ApolloClient` and passing in a `mutation` rather than a `query`. Notice that you can provide parameters for the operation as well. Just pass a second argument called `variables` to the call to `query` or `mutate`:

```js
import { withApollo, gql } from 'react-apollo'

class Chat extends Component {

  // ...

  _onSend = () => {
    this.props.client.mutate({
      mutation: gql`
        mutation createMessage($text: String!) {
          createMessage(text: $text) {
            id
            text
          }
        }
      `,
      variables: {
        text: 'Hello'
      }      
    }).then(response => {
      // handle the response
    })
  }
}

export default withApollo(Chat)
```

## Wrap Up

In today's chapter, you learned how to integrate React and Apollo to send some basic queries and mutations. Apollo makes it easy to display data with React. A component can be combined with one ore more queries and the query result is then injected into the component's props. Mutations work in a similar way where a mutation function is injected into the props of the component and can be called from there.

[Next week](!alias-die6mewitu), we'll talk about how we can make the chat messages appear on the screen in realtime using GraphQL subscriptions.

Let us know how you like the tutorial or ask any questions you might have. Contact us on [Twitter](https://twitter.com/graphcool) or join our growing community on [Slack](http://slack.graph.cool/)!

<!-- FREECOM_SIGNUP -->
