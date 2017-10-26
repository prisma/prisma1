---
alias: cu3jah9ech
description: Learn how to secure data access in your GraphQL backend and an email/password-based user authentication to your users with React and Apollo Client.
github: "https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-email-and-apollo"
---

# User Authentication with Email for React and Apollo

In this guide, you will learn how to implement an "Email & Password"-based authentication workflow with Graphcool and configure [permission rules](!alias-iegoo0heez) to control data access among your users. For the frontend, you're going to use React & Apollo Client.

You're going to build a simple Instagram clone that fulfills the following requirements:

- Upon signup, users need to provide their name, email and a password
- Everyone is able to see all the posts
- Only authenticated users are able to create new posts
- Only the author of a post can update or delete it
 
> You can find the complete example on [GitHub](https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-email-and-apollo).


## Getting started

The first thing you need to do is download the starter project for this guide.

<Instruction>

Open a terminal and download the starter project:

```sh
curl https://codeload.github.com/graphcool-examples/react-graphql/tar.gz/starters | tar -xz --strip=1 react-graphql-starters/authentication-with-email-and-apollo
cd authentication-with-email-and-apollo
```

</Instruction>

## Setting up your GraphQL server

### Installing the Graphcool CLI 

Before you can start with the actual implementation of the React app, you need to create the GraphQL server for your project. You'll do this with the [Graphcool CLI](!alias-zboghez5go).

<Instruction>

If you haven't done so yet, go ahead and install the Graphcool CLI using npm:

```bash
npm install -g graphcool
```

</Instruction>

### Creating a new Graphcool service

Now that the CLI is installed, you can use it to create the file structure for a new [Graphcool service](!alias-opheidaix3).

<Instruction>

```bash(path="server")
# Create a the file structure for a new Graphcool service in
# a directory called `server` 
graphcool init server
```

</Instruction>

You're passing `server` as the directory name to the [`init`](!alias-aiteerae6l#graphcool-init) command, the CLI will also create this directory for you and put all generated files into it.

Here is an overview of the generated files and the project structure which the CLI now created:

```(nocopy)
.
└── server
    ├── graphcool.yml
    ├── types.graphql
    └── src
        ├── hello.graphql
        └── hello.js
```

[`graphcool.yml`](!alias-foatho8aip) contains the [service definition]](!alias-opheidaix3) with all the information around your [data model](!alias-eiroozae8u) and other type definitions, usage of [functions](!alias-aiw4aimie9), [permission rules](!alias-iegoo0heez) and more.


### Adding the `email-password` authentication template

When working with Graphcool, you can easily add features to your service by pulling in a [template](!alias-zeiv8phail). 

> A Graphcool template is nothing but the definition of another Graphcool service. When running `graphcool add-template <template>`, the CLI downloads all the code from the corresponding GitHub directory and adds the functionality from the template as _comments_ to your `graphcool.ym` and `types.graphql` files.

#### Downloading the template code

For this tutorial, you'll use the [`email-password`](https://github.com/graphcool/templates/tree/master/auth/email-password) authentication template that implements simple signup and login flows.

<Instruction>

In the `server` directory, execute the following command to add the template:

```bash(path="server")
graphcool add-template graphcool/templates/auth/email-password
```

</Instruction>

> Notice that [`graphcool/templates/authentication/email-password`](https://github.com/graphcool/templates/tree/master/auth/email-password) simply corresponds to a path on GitHub. It points to the `auth/email-password` directory in the `templates` repository in the [`graphcool`](https://github.com/graphcool/) GitHub organization. This directory contains the service definition and all additional files for the Graphcool service that is your template.

#### A closer look at the `email-password` template

Let's also quickly understand what the template actually contains, here is it's file structure:

```(nocopy)
.
├── README.md
├── graphcool.yml
├── package.json
├── src
│   ├── authenticate.graphql
│   ├── authenticate.ts
│   ├── loggedInUser.graphql
│   ├── loggedInUser.ts
│   ├── signup.graphql
│   └── signup.ts
└── types.graphql
```

The most important parts for now are the service and type definitions. 

##### Service definition: `graphcool.yml` 

```yml(path="server/graphcool.yml"&nocopy)
# GraphQL types
types: ./types.graphql

# functions
functions:
  signup:
    handler:
      code:
        src: ./code/signup.js
    type: resolver
    schema: ./schemas/signup.graphql
  authenticate:
    handler:
      code:
        src: ./code/authenticate.js
    type: resolver
    schema: ./schemas/authenticate.graphql
  loggednInUser:
    type: resolver
    schema: ./schemas/loggedInUser.graphql
    handler:
      code:
        src: ./code/loggedInUser.js
```

##### Type definitions: `types.graphql` 

```graphql(path="server/types.graphql"nocopy)
type User @model {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  email: String! @isUnique
  password: String
}
```

The service definition defines three `resolver` functions you can use for signup and login functionality as well as for querying the currently logged in user. You'll take a look at the implementations in a bit.

The type definitions simply define the `User` type that you'll use to store user data and represent authenticated users.

#### Uncommenting the added lines

As mentioned above, the `add-template` command downloads the code from the GitHub repository and adds it as comments to your configuration files. In order to "activate" the functionality, you still need to uncomment the lines that were added by the CLI.

<Instruction>

Open `graphcool.yml` and uncomment the three `resolver` functions: `signup`, `login` and `loggedInUser` so the file looks as follows (you can also delete the configuration for the predefined `hello` function):

```yml(path="server/graphcool.yml")
types: ./types.graphql

functions:
  signup:
    handler:
      code:
        src: ./code/signup.js
    type: resolver
    schema: ./schemas/signup.graphql
  authenticate:
    handler:
      code:
        src: ./code/authenticate.js
    type: resolver
    schema: ./schemas/authenticate.graphql
  loggednInUser:
    type: resolver
    schema: ./schemas/loggedInUser.graphql
    handler:
      code:
        src: ./code/loggedInUser.js

permissions: 
  - operation: "*"
```

</Instruction>

Similarly, you need to uncomment the `User` type that was added to `types.graphql`.

<Instruction>

Open `types.graphql` and uncomment the added `User` type. You can also delete the `User` that was already predefined in that file. In the end, the file will only have one `User` type left, looking as follows:

```graphql(path="server/types.graphql")
type User @model {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  email: String! @isUnique
  password: String
}
```

</Instruction>

#### Deploying the service

It's now time to deploy your service! Once deployed it will be available via an HTTP endpoint that exposes the functionality defined in `graphcool.yml`. You can deploy a service using the [`graphcool deploy`](!alias-aiteerae6l#graphcool-deploy) command.

Before deployment, you still need to install the node dependencies for your `resolver` functions. These are specified in `server/package.json`.

<Instruction>

In your terminal, navigate to the `server` directory, install the node dependencies and deploy the service: 

```bash(path="server")
yarn install # or npm install
graphcool deploy
``` 

When prompted which cluster you want to deploy to, choose any of the **Backend-as-a-Service** options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`).

</Instruction>

The command outputs the HTTP endpoints of your GraphQL API. Notice that it also created the _local_ [`.graphcoolrc`](!alias-zoug8seen4) inside the current directory. This is used to manage your [deployment targets](!alias-zoug8seen4#managing-targets-in-a-local-graphcoolrc).

> **Note**: You can already start testing your API by sending queries and mutations through a GraphQL Playground. Open a Playground either by pasting the HTTP endpoint into the address bar of your browser or by using the [`graphcool playground`](!alias-aiteerae6l#graphcool-playground) command inside the `server` directory.

### Configuring the data model

In addition to the `User` that you got from the `email-password` authentication template, you also need a type to represent the posts that your users will be creating once they're authenticated. Here's what the corresponding model looks like.

<Instruction>

Open `./server/types.graphql` and add the following definition to it:

```graphql(path="server/types.graphql")
type Post {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  description: String!
  imageUrl: String!
  author: User @relation(name: "UsersPosts")
}
```

</Instruction>

The `author`-field represents the one end of the one-to-many relation between the `User` and the `Post` type. This relation represents the fact that an authenticated user can be the _author_ of a post.

<Instruction>

To add the other end of the relation, you have to update the `User` type from the template. Open `types.graphql` and update the `User` type as follows:

```graphql(path="server/types.graphql")
type User @model {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  # other template fields
  email: String @isUnique
  password: String
  
  # custom fields
  name: String!
  posts: [Post!]! @relation(name: "UsersPosts")
}
```

</Instruction>

After every change you're making to your service definition, you need to redeploy the service for the changes to actually take effect.

<Instruction>

In the `server` directory inside a terminal, invoke the following command:

```bash(path="server")
graphcool deploy
```

</Instruction>

Here's what the generated output looks like. 

```(nocopy)
$ graphcool deploy
Deploying to service __SERVICE_ID__ with local environment dev.... ✔

Your service __SERVICE_ID__ of env dev was successfully updated.\nHere are the changes:

Types

  Post
   + A new type with the name `Post` is created.
    ├── +  A new field with the name `description` and type `String!` is created.
    └── +  A new field with the name `imageUrl` and type `String!` is created.

Relations

  UsersPosts
   + The relation `UsersPosts` is created. It connects the type `Post` with the type `User`.
```

This reflects precisely the changes we mentioned above.

## Configuring Apollo Client

You're finally getting to the point where you can turn your attention towards the frontend.

The first thing you need to is add the dependencies for Apollo Client.

<Instruction>

In the root directory of your service, add the following dependencies using [yarn](https://yarnpkg.com/en/):

```bash(path="")
yarn add react-apollo
```

</Instruction>

Next you need to instantiate the `ApolloClient` and configure it with the endpoint of your GraphQL API.

<Instruction>

Open `index.js` and add the following import statements to its top:

```js(path="src/index.js")
import { ApolloClient, ApolloProvider, createNetworkInterface } from 'react-apollo'
```

Having the imports available, you can now instantiate the `ApolloClient`. Add the following code right below the import statements:

```js(path="src/index.js")
const networkInterface = createNetworkInterface({ uri: 'https://api.graph.cool/simple/v1/__SERVICE_ID__' })
const client = new ApolloClient({ networkInterface })
```

Finally, wrap the `BrowserRouter` which is currently the root of your component hierarchy inside an `ApolloProvider` which receives the `client` in its props:

```js(path="src/index.js")
ReactDOM.render((
    <ApolloProvider client={client}>
      <BrowserRouter>
        <Switch>
          <Route exact path='/' component={App} />
          <Route exact path='/create' component={CreatePost} />
          <Route exact path='/login' component={LoginUser} />
          <Route exact path='/signup' component={CreateUser} />
        </Switch>
      </BrowserRouter>
    </ApolloProvider>

  ),
  document.getElementById('root')
)
```

</Instruction>

The `ApolloClient` is your main interface to the GraphQL server and will take care of sending all queries and mutations for you. The last thing you need to do is replace the `__SERVICE_ID__` placeholder when calling `createNetworkInterface`. 

<Instruction>

To get access to your service ID, simply use the following command in the terminal:

```bash(path="server")
graphcool info
```

Then copy the value for `serviceId` and replace the `__SERVICE_ID__` placeholder from before.

</Instruction>


## Implementing the signup flow

### Adjusting the `signupUser` mutation

You'll start by implementing the signup flow in the app. Users need to provide a name, email and password to be able to create an account. 

The React component that's responsible for this functionality is implemented in `CreateUser.js` and is rendered under the `/signup` route.

Before diving into the code, take a quick look at your API, and in particular the `signupUser` mutation that was added to the service through the `email-password` template:

```graphql(nocopy)
signupUser(email: String!, password: String!): SignupUserPayload
```

And the corresponding type definition:

```(nocopy)
type SignupUserPayload{
  id: ID!
  token: String!
}
```

This mutation allows to create a new `User` by providing an email and a password. It also allows the client to directly receive an authentication token that can be used to authenticate all subsequent requests in the name of the logged in user.

There is one minor issue with this mutation at the moment, it's lacking the possibility to also provide a name when a new user is created. So, you'll have to fix that first! 

> **Note**: You could work around this problem by first calling the `signupUser` mutation and then directly invoking the `updateUser` mutation to set the new user's `name`. For this guide however, you'll adjust the API of the `signupUser` mutation and the corresponding `resolver` function in `signup.js`.

First, you need to adjust the signature of the mutation.

<Instruction>

Open `signup.graphql` and adjust the extension of the `Mutation` type as follows:

```graghql(path="server/src/email-password/signup.graphql")
extend type Mutation {
  signupUser(email: String!, password: String!, name: String!): SignupUserPayload
}
```

</Instruction>

Now, the caller of the mutation can also pass a `name` to it. To make sure that `name` also gets assigned to the new user when it's created, you also need to adjust the implementation of the corresponding serverless function.

<Instruction>

Open `signup.js` and update the `createGraphcoolUser` function to look like this:

```js{7}(path="server/src/email-password/signup.js")
function createGraphcoolUser(api, email, passwordHash, name) {
  return api.request(`
    mutation {
      createUser(
        email: "${email}",
        password: "${passwordHash}",
        name: "${name}"
      ) {
        id
      }
    }`)
    .then((userMutationResult) => {
      return userMutationResult.createUser.id
    })
}
```

</Instruction>

Finally, you also need to adjust the call to the `createGraphcoolUser` and provide the `name` argument that you can extract from the input `event`.

<Instruction>

Still in `signup.js`, add the required changes to the exported function:

```js{9,19}(path="server/src/email-password/signup.js")
template.exports = function(event) {
  if (!event.context.graphcool.pat) {
    console.log('Please provide a valid root token!')
    return { error: 'Email Signup not configured correctly.'}
  }

  const email = event.data.email
  const password = event.data.password
  const name = event.data.name
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')
  const SALT_ROUNDS = 10

  if (validator.isEmail(email)) {
    return getGraphcoolUser(api, email)
      .then(graphcoolUser => {
        if (graphcoolUser === null) {
          return bcrypt.hash(password, SALT_ROUNDS)
            .then(hash => createGraphcoolUser(api, email, hash, name))
        } else {
          return Promise.reject("Email already in use")
        }
      })
      .then(graphcoolUserId => {
        return graphcool.generateAuthToken(graphcoolUserId, 'User')
          .then(token => {
            return { data: {id: graphcoolUserId, token}}
        })
      })
      .catch((error) => {
        console.log(error)

        // don't expose error message to client!
        return { error: 'An unexpected error occured.' }
      })
  } else {
    return { error: "Not a valid email" }
  }
}
```

</Instruction>

Finally, these changes need to be deployed again.

<Instruction>

Navigate to the `server` directory in a terminal and execute the `deploy` command:

```bash(path="server")
graphcool deploy
```

</Instruction>

Awesome, your `signupUser` mutation now also accepts a `name` argument and assigns its value to the newly created `User` node right away. 💪

### Calling the `signupUser` mutation

You'll now make use of this mutation by adding it to the `CreateUser` component with Apollo's `graphql` higher-order component.

<Instruction>

Open `CreateUser.js` and add the following code to the bottom of the file, also replacing the current export statement:

```js(path="src/components/CreateUser.js")
const SIGNUP_EMAIL_USER = gql`
  mutation SignupUser($email: String!, $password: String!, $name: String) {
    signupUser(email: $email, password: $password, name: $name) {
      id
      token
    }
  }
`

export default graphql(SIGNUP_EMAIL_USER, {name: 'signupUserMutation'})(withRouter(CreateUser))
```

For this code to work, you also need to import `graphql` and `gql`, so add the following import statement to the top of the file:

```js(path="src/components/CreateUser.js")
import { graphql, gql } from 'react-apollo'
```

</Instruction>

`SIGNUP_EMAIL_USER` is the mutation. You're then adding the mutation to the component by wrapping it with a call to `graphql`. The `name` argument that you're providing determines the name of the function that Apollo now injects into the props of your component.

Finally, you actually need to call the mutation and provide the arguments that you're extracting from the component's `state`.

<Instruction>

Still in `CreateUser.js`, implement the `createUser` function like so:

```js(path="src/components/CreateUser.js")
createUser = async () => {
  const { email, password, name } = this.state

  try {
    const response = await this.props.signupUserMutation({variables: {email, password, name}})
    localStorage.setItem('graphcoolToken', response.data.signupUser.token)
    this.props.history.push('/')
  } catch (e) {
    console.error('An error occured: ', e)
    this.props.history.push('/')
  }

}
```

</Instruction>

You're simply calling the `signupUserMutation` and store the returned authentication `token` in `localStorage`.

You can now test the authentication by running the app with `yarn start` and navigating the the `http://localhost:3000/signup` route. After having added a new user with email, password and name, you can can verify that the user actually was created by opening the Graphcool console and checking the data browser:

![](https://imgur.com/NNxHu2Z.png)

![](https://imgur.com/LdV5ud7.png)


## Implementing the authentication flow

Now that the signup flow is out of the way, you can implement the actual login. It will work in a very similar way as the signup, except that this time you don't have to change anything about the API of the `authenticateUser` mutation that you get from the `email-password` template.

<Instruction>

Open `LoginUser.js` and add the `authenticateUser` mutation to its bottom, again also replacing the current export statement:

```js(path="src/components/LoginUser.js")
const AUTHENTICATE_EMAIL_USER = gql`
  mutation AuthenticateUser($email: String!, $password: String!) {
    authenticateUser(email: $email, password: $password) {
      token
    }
  }
`

export default graphql(AUTHENTICATE_EMAIL_USER, {name: 'authenticateUserMutation'})(withRouter(LoginUser))
```

For this work, don't forget to add the required import statements to the top of the file:

```js(path="src/components/LoginUser.js")
import { graphql, gql } from 'react-apollo'
```

</Instruction>

This works in the same way as before: Apollo now injects the mutation function into the props of your component and you can call it by the `name` that's provided to the call to `graphql`.

Lastly, you need to actually call the mutation.

<Instruction>

Still in `LoginUser.js`, implement the `loginUser` function as follows:

```js(path="src/components/LoginUser.js")
loginUser = async () => {
  const { email, password } = this.state

  try {
    const response = await this.props.authenticateUserMutation({variables: { email, password }})
    localStorage.setItem('graphcoolToken', response.data.authenticateUser.token)
    this.props.history.push('/')
  } catch (e) {
    console.error('An error occured: ', e)
    this.props.history.push('/')
  }

}
```

</Instruction>

With this code in place, you can now test the authentication by running the app and navigating to the `http://localhost:3000/login` route.

## Checking the authenticated status

In the app, you want to be able to detect whether a user is currently logged in. A very simple way to do so would be to simply check whether `localStorage` contains a value for the key `graphcoolToken`, since that's how you're storing the authentication tokens after having received them from your API.

Notice however that these tokens are _temporary_, meaning they'll eventually expire and can't be used for authentication any more. This means that ideally you should not only check whether you currently have a token available in `localStorage`, but actually validate it _against the API_ to confirm that it's either valid or expired.

> The default validity duration of a token is 30 days. However, when issuing a token in the `signup` or `login` resolver, you can explicitly pass in the validity duration you'd like to have for your application. Read more about this in the [docs](!alias-eip7ahqu5o#node-tokens) 

For exactly this purpose, the `email-password` template provides a dedicated query that you can send to the API, with an authentication token attached to the request, and the server will return the `id` of a logged-in user or `null` if the token is not valid.

Here's what the query looks like:

```graphql(nocopy)
extend type Query {
  loggedInUser: LoggedInUserPayload!
}

type LoggedInUserPayload {
  # if `id` is `null`, it means there is not logged in user
  id: ID
}
```

You want this information to be available in the root of your application, that's the `App` component.

<Instruction>

Open `App.js` and add the `loggedInUser` query to the `App` component, again by also replacing the current export statement:

```js(path="src/components/App.js")
const LOGGED_IN_USER = gql`
  query LoggedInUser {
    loggedInUser {
      id
    }
  }
`

export default graphql(LOGGED_IN_USER, { options: {fetchPolicy: 'network-only'}})(withRouter(App))
```

Again, make sure to import `gql` and `graphql` on top of the file to make this code work:

```js(path="src/components/App.js")
import { gql, graphql } from 'react-apollo'
```

</Instruction>

Whenever the `App` component now loads, Apollo executes the `loggedInUser` query against the API of your Graphcool service. So now you need to make sure that the result of the query is used accordingly to render the UI of your app: If the query was successful and returned the `id` of a logged-in user, you want to display a logout-button as well as a button for the user to create a new post. Otherwise, you simply render the same UI as before with the login- and signup-buttons.

Notice you're specifying the `fetchPolicy` when you're adding the `loggedInUser` query to the `App` component. This is to make sure that Apollo actually executes the request against the API rather than looking up a previous result in its cache.

<Instruction>

To properly render the login-state of the user, first implement the `_isLoggedIn` function inside `App.js` function properly:

```js(path="src/components/App.js")
_isLoggedIn = () => {
  return this.props.data.authenticatedUser && this.props.data.authenticatedUser.id !== ''
}
```

You also want to display the user's `id` in case they're actually logged in. Update the `renderLoggedIn` function as follows:

```js{5}(path="src/components/App.js")
renderLoggedIn() {
  return (
    <div>
      <span>
        Logged in as ${this.props.data.loggedInUser.id}
      </span>
      <div className='pv3'>
        <span
          className='dib bg-red white pa3 pointer dim'
          onClick={this._logout}
        >
          Logout
        </span>
      </div>
      <ListPage />
      <NewPostLink />
    </div>
  )
}
```

</Instruction>

Lastly, to also account for the ongoing network request, you should make sure to render a loading-state as long as your users are waiting for a response from the server.

<Instruction>

Still in `App.js`, update `render` to look as follows:

```js{3-5}(path="src/components/App.js")
render () {

  if (this.props.data.loading) {
    return (<div>Loading</div>)
  }

  if (this._isLoggedIn()) {
    return this.renderLoggedIn()
  } else {
    return this.renderLoggedOut()
  }

}
```

</Instruction>

This is all the code you need in order to implement the logged-in status. However, when running the app you'll find that despite the fact that you are already logged in (at least that's the case if you've create a new user before), the UI still looks as before and doesn't render neither the logout-button, nor the button for the user to create new posts.

That's because the token is not yet attached to the request, so your GraphQL server still doesn't actually know in whose name the reuqest is sent! 

To attach the token to the request's header, you need to configure your `ApolloClient` instance accordingly, since it is responsible for sending all the HTTP requests that contain your queries and mutations.

<Instruction>

Open `index.js` and add the following configuration right before you're instantiating the `ApolloClient`:

```js(path="src/index.js")
networkInterface.use([{
  applyMiddleware (req, next) {
    if (!req.options.headers) {
      req.options.headers = {}
    }

    // get the authentication token from local storage if it exists
    if (localStorage.getItem('graphcoolToken')) {
      req.options.headers.authorization = `Bearer ${localStorage.getItem('graphcoolToken')}`
    }
    next()
  },
}])
```

</Instruction>

With this setup, Apollo will now make sure the `Authorization` header of your HTTP requests contains the auth token.

You can now test the functionality. Run the app by calling `yarn start`, make sure you're logged in with an existing user and verify that the UI renders the logged-in state:

![](https://imgur.com/oHASvqS.png)

## Displaying posts

To display all posts, you simply need to send the `allPosts` query to the API and render the results in a list inside the `ListPage` component.

<Instruction>

Open `ListPage.js` and add the following query along with the export statement to the bottom of the file:

```js(path="src/components/ListPage.js")
const ALL_POSTS = gql`
  query AllPostsQuery {
    allPosts(orderBy: createdAt_DESC) {
      id
      imageUrl
      description
    }
  }
`

export default graphql(ALL_POSTS)(ListPage)
```

Then import `gql` and `graphql`:

```js(path="src/components/ListPage.js")
import { gql, graphql } from 'react-apollo'
```

And finally update `render` to display the downloaded posts:

```js(path="src/components/ListPage.js")
render () {
  if (this.props.data.loading) {
    return (<div>Loading</div>)
  }

  return (
    <div className='w-100 flex justify-center'>
      <div className='w-100' style={{ maxWidth: 400 }}>
        {this.props.data.allPosts.map(post =>
          <Post key={post.id} post={post} />
        )}
      </div>
    </div>
  )
}
```

</Instruction>

That's it! Your app now fetches all the posts and renders them initially.

## Creating new posts

The last bit of functionality that's needed in the app is the feature to add new posts. This will be implemented by using the `createPost` mutation which is part of the CRUD API that Graphcool automatically generates for your model types.

<Instruction>

Open `CreatePost.js` and add the following mutation to the bottom of the file, like before also replacing the current export statement:

```js(path="src/components/CreatePost.js")
const CREATE_POST = gql`
  mutation CreatePost ($description: String!, $imageUrl: String!, $authorId: ID!) {
    createPost(description: $description, imageUrl: $imageUrl, authorId: $authorId) {
      id
    }
  }
`

export default graphql(CREATE_POST, {name: 'CreatePostMutation'})(withRouter(CreatePost))
```

Then import `gql` and `graphql` on top of the file:

```js(path="src/components/CreatePost.js")
import { gql, graphql } from 'react-apollo'
```

And finally, call the mutation inside `handlePost`:

```js(path="src/components/CreatePost.js")
handlePost = async () => {
  const loggedInUser = // ... where to get the logged in user from?

  // redirect if no user is logged in
  if (!loggedInUser) {
    console.warn('Only logged in users can create new posts')
    this.props.history.push('/')
    return
  }

  const { description, imageUrl } = this.state
  const authorId = loggedInUser.id

  await this.props.CreatePostMutation({variables: { description, imageUrl, authorId }})
  this.props.history.push('/')
}
```

</Instruction>

All right, this code will work but one detail is still missing! You need to have access to the currently logged-in user to be able to send the mutation (since you need to provide the user's `id` as the `authorId` argument when sending the mutation to the server). 

To get access to the currently logged-in user, you'll simply use the `loggedInUser` query again, in the same way you already did in `App.js`. 

<Instruction>

In `CreatePost.js`, add the `loggedInUser` query to the bottom of the file (right before the export statement):

```js(path="src/components/CreatePost.js")
const LOGGED_IN_USER = gql`
  query LoggedInUser {
    loggedInUser {
      id
    }
  }
`
```

Now, you can use the `compose` function to easily add multiple GraphQL operations to the component. Adjust the current export statement to look like this:

```js(path="src/components/CreatePost.js")
export default compose(
  graphql(CREATE_POST, {name: 'CreatePostMutation'}),
  graphql(LOGGED_IN_USER, {fetchPolicy: 'network-only'}),
)(withRouter(CreatePost))
```

And finally make sure to also import the `compose` function from the `react-apollo` package:

```js(path="src/components/CreatePost.js")
import { gql, graphql, compose } from 'react-apollo'
```

Now you can simply adjust the line inside `handlePost` to actually retreive the logged-in user from the component's props:

```js(path="src/components/CreatePost.js")
const loggedInUser = this.props.data.loggedInUser
```

</Instruction>

Great, your users are now able to create new posts once they're logged in! 🎉

![](https://imgur.com/dc0xx9U.png)
*Creating a new post with the `CreatePost` component.*

![](https://imgur.com/SlDcDMW.png)
*The new post is now displayed in the `ListPage` component.*


## Configuring permission rules

All the features are now implemented, but wait - didn't we have more requirements for the app?

We also want to _secure_ data access and configure [permission rules](!alias-iegoo0heez) such that only authenticated users can create new posts, and only the author of a post can update or delete it. Right now, _everyone_ who has access to the endpoint of your GraphQL server is able to perform these operations (despite the fact that the UI of your React app of doesn't allow this)!


### Permissions in Graphcool

When using Graphcool, you need to explicitly allow your clients to perform the operations that are exposed by your API. But wait! If that's the case, why were you able to create and download posts before then? Shouldn't you have had to explicitly allow these operations then?

The reason why the `allPosts` query and `createPost` mutation were already working is simple. When a new Graphcool service is created, there is a _wildcard permission_ setup for you that does the job of allowing _all_ operations.

All permission rules need to be defined in the service definition, the `graphcool.yml`-file. If you check this file now, you'll only see one permission that's currently part of your service and that was initially added by the Graphcool CLI:

```yml(path="server/graphcool.yml&nocopy")
permissions:
  - operation: '*'
```

This simply expresses that all operations are allowed.

> If you remove this one permission and run `graphcool deploy` afterwards, you'll notice that all your queries and operations will fail with a _Insufficient permissions_ error.

Generally, the `permissions` property in the service definition contains a list of _permissions_. A single permission has the following properties:

- `operation` (required): Specifies for which API operation (query or mutation) this permissions applies
- `authenticated` (optional, default: `false`): Indicates whether a client who wants to perform this operation needs to be authenticated
- `query` (optional): Points to a file that contains a permission query defining the rules for who is allowed to perform this operation

<Instruction>

To start with a clean slate, remove the `- operation: '*'` from the `graphcool.yml` file.

</Instruction>

### Allowing all users to see posts

The first permission you're going to add is the one to allow everyone to see posts.

<Instruction>

In the `graphcool.yml`-file, add the following permission under the `permissions`-section:

```yml(path="server/graphcool.yml")
- operation: Post.read
```

</Instruction>

### Allowing only authenticated users to create posts

For the next permission to allow only authenticated users to create new posts, you'll make use of the `authenticated` property.

<Instruction>

Still in `graphcool.yml`, add the following permissions right below the previous one:

```yml(path="server/graphcool.yml")
- operation: Post.create
  authenticated: true
- operation: UsersPosts.connect
  authenticated: true
```

</Instruction>

This now expresses that users who are trying to perform the `createPost` mutation need to be authenticated. The second permission for `UsersPosts` is needed because the `createPost` mutation will also create a relation (which is called `UsersPosts` as defined in `types.graphql`), so the `connect` operation on the relation needs to be allowed as well.

### Allowing only the authors of a post to udpate and delete it

For these permissions, you need to make use of a [permission query](!alias-iox3aqu0ee). Permission queries are regular GraphQL queries that only return `true` or `false`. If you specify a permission query for an operation, Graphcool will run the permission query right before the corresponding operation is performed. Only if the query then returns `true`, the operation will actually be performed, otherwise it fails with a permission error.

<Instruction>

First, add the permissions to the service definition:

```yml(path="server/graphcool.yml")
- operation: Post.update
  authenticated: true
  query: src/permissions/Post.graphql
- operation: Post.delete
  authenticated: true
  query: src/permissions/Post.graphql
```

</Instruction>

Both permissions require the user to be authenticated, but they also point to a file that contain more concrete rules for these operations. So, you'll need to create these files next!

<Instruction>

Create a new directory inside `server/src` and call it `permissions`.

Then create a new file inside that new directory, call it `Post.graphql`.

Finally, add the following permission query to it:

```graphql(path="server/permissions/Post.graphql")
query ($node_id: ID!, $user_id: ID!) {
  SomePostExists(
    filter: {
      id: $node_id,
      author: {
        id: $user_id
      }
    }
  )
}
```

</Instruction>

Notice that the arguments `$node_id` and `$user_id` will be injected into the query automatically when it is executed. `$node_id` is the `id` of the `Post` node that is to be updated (or deleted), and `$user_id` is the `id` of the currently logged in user who's sending the request.

With that knowledge, you can derive the meaning of the permission query. It effecticely requires two things:

1. There needs to exist a `Post` node with its `id` being equal to `$node_id`
2. The `id` of the `author` of that `Post` node needs to be equal to `$user_id`

Lastly, you need to make sure the changes are applied to your service.

<Instruction>

In the `server` directory, execute the following command in the terminal:

```bash(path="server")
graphcool deploy
```

</Instruction>
 
Awesome! Now the permission rules apply and all our initial requirements for the app are fulfilled!

## Summary

In this guide, you learned how to build a simple app using an email-password based authentication workflow.

You created your GraphQL server from scratch using the Graphcool CLI and customized the [`email-password`](https://github.com/graphcool/templates/tree/master/authentication/email-password) authentication template according to your needs.

You then configured Apollo Client inside your React app and implemented all required operations. Finally you removed the wildcard permission from the project and explicitly defined permission rules for the operations that your API exposes.


