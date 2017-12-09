---
alias: yi9jeuwohl
description: Learn how to secure data access in your GraphQL backend and an Facebook-based user authentication to your users with React and Apollo Client.
github: "https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-facebook-and-apollo"
---

# User Authentication with Facebook for React and Apollo

In this guide, you will learn how to implement a "Login-with-Facebook" authentication workflow with Graphcool and configure permission rules to control data access among your users. For the frontend, you're going to use React & Apollo Client.

You're going to build a simple Instagram clone that fulfills the following requirements:

- For signup and login, users are redirected to Facebook to authorize the application
- Everyone is able to see all the posts
- Only authenticated users are able to create new posts
- Only the author of a post can update or delete it
 
> You can find the complete example on [GitHub](https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-facebook-and-apollo).


## Getting started

The first thing you need to do is download the starter project for this guide.

<Instruction>

Open a terminal and download the starter service:

```sh
curl https://codeload.github.com/graphcool-examples/react-graphql/tar.gz/starters | tar -xz --strip=1 react-graphql-starters/authentication-with-facebook-and-apollo
cd authentication-with-facebook-and-apollo
```

</Instruction>


## Setting up your GraphQL server

### Installing the Graphcool CLI 

Before you can start with the actual implementation of the React app, you need to create the GraphQL server your app will run against. You'll create the server using the [Graphcool CLI](!alias-zboghez5go).

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

### Adding the `facebook` authentication template

When working with Graphcool, you can easily add features to your service by pulling in a [template](!alias-zeiv8phail). 

> A Graphcool template is nothing but the definition of another Graphcool service. When running `graphcool add-template <template>`, the CLI downloads all the code from the corresponding GitHub directory and adds the functionality from the template as _comments_ to your `graphcool.ym` and `types.graphql` files.

#### Downloading the template code

For this tutorial, you'll use the [`facebook`](https://github.com/graphcool/templates/tree/master/auth/facebook) authentication template that implements the "Login with Facebook" flow.

<Instruction>

In the `server` directory, execute the following command to add the template:

```bash(path="server")
graphcool add-template graphcool/templates/auth/facebook
```

</Instruction>

> Notice that [`graphcool/templates/auth/facebook`](https://github.com/graphcool/templates/tree/master/authentication/facebook) simply corresponds to a path on GitHub. It points to the `auth/facebook` directory in the `templates` repository in the [`graphcool`](https://github.com/graphcool/) GitHub organization. This directory contains the service definition and all additional files for the Graphcool service that is your template.

#### A closer look at the `facebook` template

Let's also quickly understand what the template actually contains, here is it's file structure:

```(nocopy)
.
├── README.md
├── docs
│   ├── app-id.png
│   └── facebook-login-settings.png
├── graphcool.yml
├── login.html
├── package.json
├── src
│   ├── facebookAuthentication.graphql
│   ├── facebookAuthentication.ts
│   ├── loggedInUser.graphql
│   └── loggedInUser.ts
└── types.graphql
```

The most important parts for now are the service and type definitions. 

##### Service definition: `graphcool.yml` 

```yml(path="server/graphcool.yml"&nocopy)
types: ./types.graphql

functions:
  facebookAuthentication:
    handler:
      code:
        src: ./src/facebookAuthentication.ts
    type: resolver
    schema: ./src/facebookAuthentication.graphql
  loggedInUser:
    handler:
      code:
        src: ./src/loggedInUser.ts
    type: resolver
    schema: ./src/loggedInUser.graphql
```

##### Type definitions: `types.graphql` 

```graphql(path="server/types.graphql"&nocopy)
type User @model {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  facebookUserId: String @isUnique
  email: String # optional, because it's obtained from Facebook API
}
```

The service definition defines two `resolver` functions. The first one, `facebookAuthentication` is used for the signup and login functionality. The second one, `loggedInUser` allows to validate whether an authentication token belongs to a currently logged in user in the Graphcool API. You'll take a look at the implementations in a bit.

The type definitions simply define the `User` type that you'll use to store user data and represent authenticated users.

#### Uncommenting the added lines

As mentioned above, the `add-template` command downloads the code from the GitHub repository and adds it as comments to your configuration files. In order to "activate" the functionality, you still need to uncomment the lines that were added by the CLI.

<Instruction>

Open `graphcool.yml` and uncomment the two `resolver` functions: `facebookAuthentication` and `loggedInUser` so the file looks as follows (you can also delete the configuration for the predefined `hello` function):

```yml(path="server/graphcool.yml")
types: ./types.graphql

functions:
  facebookAuthentication:
    handler:
      code:
        src: ./src/facebookAuthentication.ts
    type: resolver
    schema: ./src/facebookAuthentication.graphql
  loggedInUser:
    handler:
      code:
        src: ./src/loggedInUser.ts
    type: resolver
    schema: ./src/loggedInUser.graphql

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

  facebookUserId: String @isUnique
  email: String # optional, because it's obtained from Facebook API
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

### Configuring the data model

In addition to the `User` that you got from the `facebook` authentication template, you also need a type to represent the posts that your users will be creating once they're authenticated. Here's what the corresponding model looks like.

<Instruction>

Open `./graphcool/types.graphql` and add the following definition to it:

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

```graphql(path="server/templates/facebook/types.graphql")
type User @model {
  # Required system field:
  id: ID! @isUnique # read-only (managed by Graphcool)

  # Optional system fields (remove if not needed):
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  # other template fields
  facebookUserId: String @isUnique
  facebookEmail: String
  
  # custom fields
  posts: [Post!]! @relation(name: "PostsByUser")
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

## Connecting the App with Facebook

The "Login with Facebook" authentication works in the way that your app will be receiving an authentication token from the Facebook API that proves your users' identities. In order for this flow to work, you need to first create a _Facebook app_.

### Creating a Facebook app

<Instruction>

Follow the [instructions in the Facebook documentation](https://developers.facebook.com/docs/apps/register) to create your own Facebook app.

</Instruction>

Once your app was created, you need to enable _Facebook Login_ and configure it with the right information.

<Instruction>

Select **Facebook Login** in the left sidebar (listed under **PRODUCTS**) and add the following URLs to the **Valid OAuth redirects URIs**: `http://localhost:3000`.

![](https://imgur.com/pTkB4sX.png)

</Instruction>


### Configuring the Facebook SDK

The Facebook SDK is already contained in the starter service, it's loaded asynchronously using a script inside `componentDidMount` of the `App` component. However, you still need to configure it with the information about your particular app.

<Instruction>

Open the **Dashboard** in the sidebar of your Facebook app and copy the **App ID** as well as the **API Version** into `App.js`. Set them as the values for the two constants `FACEBOOK_APP_ID` and `FACEBOOK_API_VERSION` which are defined on top of the file.

![](https://imgur.com/L7b8GCn.png)

</Instruction> 

That's it - your app is now ready to use the Facebook login! 🎉


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


## Implementing Facebook authentication

### The "Login with Facebook" flow

Here's what's supposed to happen when the user wants to authenticate themselves with Facebook in the app:

1. The user clicks the **Login with Facebook** button
2. The Facebook UI is loaded and the user accepts
3. The app receives a _Facebook access token_ (inside `_facebookCallback` in `App.js`)
4. Your app calls the Graphcool mutation `authenticateUser(facebookToken: String!)`
5. If no user exists yet that corresponds to the passed Facebook access token, a new `User` node will be created
6. In any case, the `authenticateUser(facebookToken: String!)` mutation returns a valid token for the user
7. Your app stores the token and stores it in `localStorage` where it can be accessed by `ApolloClient` and used to authenticate all subsequent requests

### Creating a new `User`

The first three steps are of the authentication flow are effectively taken care of by the Facebook SDK, it's now your task to add the required functionality on the Graphcool end. Step 4 stays you need to call the `authenticateUser(facebookToken: String!)` mutation with the token that your received from Facebook, so that's what you'll do next!

<Instruction>

Open `App.js` and add the following mutation to the bottom of the file, also replacing the current export statement:

```js(path="src/components/App.js)
const AUTHENTICATE_FACEBOOK_USER = gql`
  mutation AuthenticateUserMutation($facebookToken: String!) {
    authenticateUser(facebookToken: $facebookToken) {
      token
    }
  }
`

export default graphql(AUTHENTICATE_FACEBOOK_USER, { name: 'authenticateUserMutation' })(withRouter(App))
```

For this code to work you also need to add the following import to the top of the file:

```js(path="src/components/App.js)
import { gql, graphql } from 'react-apollo'
```

</Instruction>

By using Apollo's higher-order component `graphql`, you're "combining" your React component with the `authenticateUser`-mutation. Apollo will now inject a function called `authenticateUserMutation` into the props of your component that will send the given mutation to the API for you.

The last thing you need to do to make the authentication flow work, is actually call that function.

<Instruction>

Still in `App.js`, adjust the implementation of `_facebookCallback` to look as follows:

```js(path="src/components/App.js)
_facebookCallback = async facebookResponse => {
  if (facebookResponse.status === 'connected') {
    const facebookToken = facebookResponse.authResponse.accessToken
    const graphcoolResponse = await this.props.authenticateUserMutation({variables: { facebookToken }})
    const graphcoolToken = graphcoolResponse.data.authenticateUser.token
    localStorage.setItem('graphcoolToken', graphcoolToken)
    window.location.reload()
  } else {
    console.warn(`User did not authorize the Facebook application.`)
  }
}
```

</Instruction>

That's it, the Facebook authentication now is already implemented. If you run the app and then click the "Login with Facebook"-button, a new `User` will be created in the database. You can verify that in the Graphcool console or a Playground.


## Checking the authenticated status

In the app, you want to be able to detect whether a user is currently logged in. A very simple way to do so would be to simply check whether `localStorage` contains a value for the key `graphcoolToken`, since that's how you're storing the authentication tokens after having received them from your API.

Notice however that these tokens are _temporary_, meaning they'll eventually expire and can't be used for authentication any more. This means that ideally you should not only check whether you currently have a token available in `localStorage`, but actually validate it _against the API_ to confirm that it's either valid or expired.

> The default validity duration of a token is 30 days. However, when issuing a token in the `facebookAuthentication` resolver, you can explicitly pass in the validity duration you'd like to have for your application. Read more about this in the [docs](!alias-eip7ahqu5o#node-tokens) 

For exactly this purpose, the `ƒacebook` template provides a dedicated query that you can send to the API, with an authentication token attached to the request, and the server will return the `id` of a logged-in user or `null` if the token is not valid.

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

```js{}(path="src/components/App.js")
const LOGGED_IN_USER = gql`
  query LoggedInUser {
    loggedInUser {
      id
    }
  }
`

export default compose(
  graphql(AUTHENTICATE_FACEBOOK_USER, { name: 'authenticateUserMutation' }),
  graphql(LOGGED_IN_USER, { options: { fetchPolicy: 'network-only'}})
) (withRouter(App))
```

To make this work you now need to also import the `compose` function from the `react-apollo` package:

```js{}(path="src/components/App.js")
import { gql, graphql, compose } from 'react-apollo'
```

</Instruction>

> Apollo's `compose` function allows to easily inject multiple queries and mutations into a single React component.

Whenever the `App` component now loads, Apollo executes the `loggedInUser` query against the API. So now you need to make sure that the result of the query is used accordingly to render the UI of your app. If the query was successful and returned the `id` of a logged-in user, you want to display a logout-button as well as a button for the user to create a new post. Otherwise, you simply render the same UI as before with the login- and signup-buttons.

Notice you're specifying the `fetchPolicy` when you're adding the `loggedInUser` query to the `App` component. This is to make sure that Apollo actually executes the request against the API rather than looking up a previous result in its cache.

<Instruction>

To properly render the login-state of the user, first implement the `_isLoggedIn` function inside `App.js` function properly:

```js{}(path="src/components/App.js")
_isLoggedIn = () => {
  return this.props.data.loggedInUser && this.props.data.loggedInUser.id !== ''
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

Now, you can use the `compose` function again to easily add multiple GraphQL operations to the component. Adjust the current export statement to look like this:

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

In this guide, you learned how to build a simple app using an Facebook-based authentication workflow.

You created your GraphQL server from scratch using the Graphcool CLI and customized the [`facebook`](https://github.com/graphcool/templates/tree/master/authentication/facebook) authentication template according to your needs by adding a relation to the `Post` type.

You then configured Apollo Client inside your React app and implemented all required operations. Finally you removed the wildcard permission from the service and explicitly defined permission rules for the operations that your API exposes.


