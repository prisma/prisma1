---
alias: uh5atohthu
description: Learn how to secure data access in your GraphQL backend and an Firebase-based user authentication to your users with React and Apollo Client.
github: "https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-firebase-and-apollo"
---

# User Authentication with Firebase for React and Apollo


<InfoBox type=warning>

**Note**: This guide is outdated! It uses the deprecated concept of _modules_ to pull in the authentication functionality.

_Modules_ have been replaced by _templates_. A template is a predefined Graphcool service that you can _copy_ into your project. 

Read more about templates in the [documentation](!alias-zeiv8phail) or in this [GitHub issue](https://github.com/graphcool/graphcool/issues/720) that contains the proposal and discusssion for how exactly templates should work.

An updated version of this guide is coming soon, stay tuned!

</InfoBox>

In this guide, you will learn how to use Firebase to implement authentication with Graphcool and configure permission rules to control data access among your users. For the frontend, you're going to use React & Apollo Client.

You're going to build a simple Instagram clone that fulfills the following requirements:

- For signup and login, users are redirected to Firebase to authorize the application (you can choose which Firebase login method you want to use, e.g. _Google Login_ or _Phone Authentication_)
- Everyone is able to see all the posts
- Only authenticated users are able to create new posts
- Only the author of a post can update or delete it
 
> You can find the complete example on [GitHub](https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-firebase-and-apollo).


## Getting started

The first thing you need to do is download the starter project for this guide.

<Instruction>

*Open a terminal and download the starter project:*

```sh
curl https://codeload.github.com/graphcool-examples/react-graphql/tar.gz/starters | tar -xz --strip=1 react-graphql-starters/authentication-with-firebase-and-apollo
cd authentication-with-firebase-and-apollo
```

</Instruction>


## Setting up your GraphQL server

### Installing the Graphcool CLI 

Before you can start with the actual implementation of the React app, you need to create the GraphQL server your app will run against. You'll create the server using the [Graphcool CLI](!alias-zboghez5go).

<Instruction>

*If you haven't done so yet, go ahead and install the Graphcool CLI using npm:*

```bash
npm install -g graphcool
```

</Instruction>

### Creating a new Graphcool project


<Instruction>

Now that the CLI is installed, you can use it to create a new project.

```bash(path="graphcool")
graphcool init graphcool --template blank
```

</Instruction>

You're using the `--template blank` option which just creates an empty Graphcool project for you. Since you're passing `graphcool` as the directory name to the `init` command, the CLI will also create this directory for you and put all generated files into it.

Here is an overview of the generated files and the project structure which the CLI now created:

```(nocopy)
├─ .graphcoolrc
├─ code
│  ├─ hello.graphql
│  └─ hello.js
├─ graphcool.yml
└─ types.graphql
```

`graphcool.yml` contains the _project definition_ with all the information around your data model and other type definitions, usage of serverless functions, permission rules and more.


### Adding the `firebase` authentication module

When working with Graphcool, you can easily add features to your project by pulling in a _module_. 

> A Graphcool module is nothing but another Graphcool project. When running `graphcool modules add <module>`, the CLI simply downloads all the code from the corresponding GitHub directory and puts it into your project inside a directory called `modules`.

#### Installing the module

For this tutorial, you'll use the [`firebase`](https://github.com/graphcool/modules/tree/master/authentication/firebase) authentication module that offers simple signup and login flows.

<Instruction>

*In the `graphcool` directory, execute the following command to add the module:*

```bash(path="graphcool")
graphcool modules add graphcool/modules/authentication/firebase
```

</Instruction>

> Notice that [`graphcool/modules/authentication/firebase`](https://github.com/graphcool/modules/tree/master/authentication/firebase) simply corresponds to a path on GitHub. It points to the `authentication/firebase` directory in the `modules` repository in the [`graphcool`](https://github.com/graphcool/) GitHub organization. This directory contains the project definition and all additional files for the Graphcool project that is your module.

#### A closer look at the `firebase` module

Let's also quickly understand what the module actually contains, here is it's file structure:

```(nocopy)
.
├── README.md
├── code
│   ├── authenticateFirebaseUser.graphql
│   ├── authenticateFirebaseUser.js
│   ├── loggedInUser.graphql
│   └── loggedInUser.js
├── graphcool.yml
└── types.graphql
```

The most important parts for now are the project and type definitions. 

##### Project definition: `graphcool.yml` 

```yml(path="graphcool/graphcool.yml"&nocopy)
types: ./types.graphql

functions:
  authenticateFirebaseUser:
    handler:
      code:
        src: ./code/authenticateFirebaseUser.js
    type: resolver
    schema: ./code/authenticateFirebaseUser.graphql
  loggedInUser:
    handler:
      code:
        src: ./code/loggedInUser.js
    type: resolver
    schema: ./code/loggedInUser.graphql

rootTokens:
```

##### Type definitions: `types.graphql` 

```graphql(path="graphcool/types.graphql"&nocopy)
type FirebaseUser {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  firebaseUserId: String! @isUnique
}
```

The project definition defines two `resolver` functions. The first one, `authenticateFirebaseUser` is used for the signup and login functionality. The second one, `loggedInUser` allows to validate whether an authentication token belongs to a currently logged in user in the Graphcool API. You'll take a look at the implementations in a bit.

The type definitions simply define the `FirebaseUser` user type that you're going to use to represent authenticated users.


### Configuring the data model

In addition to the `FirebaseUser` that you got from the `firebase` authentication module, you also need a type to represent the posts that your users will be creating in the app once they're authenticated. Here's what the corresponding model looks like.

<Instruction>

*Open `./graphcool/types.graphql` and add the following definition to it:*

```graphql(path="graphcool/types.graphql")
type Post {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  description: String!
  imageUrl: String!
  author: FirebaseUser @relation(name: "PostsByUser")
}
```

</Instruction>

The `author`-field represents the one end of the one-to-many relation between the `FirebaseUser` and the `Post` type. This relation represents the fact that an authenticated user can be the _author_ of a post.

<Instruction>

*To add the other end of the relation, you have to update the `FirebaseUser` type. Open `/graphcool/modules/firebase/types.graphql` and update the `FirebaseUser` type as follows:*

```graphql(path="graphcool/modules/firebase/types.graphql")
type FirebaseUser implements Node {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  
  # custom fields
  posts: [Post!]! @relation(name: "PostsByUser")
}
```

</Instruction>

Notice that the CLI doesn't care about _where_ (in which files) you're putting your type definitions. When applying the changes, it'll simply merge all existing `.graphql`-files in your project and treat them as a single one.


### Deploying your changes

You made two major local changes that you now need to apply to the "remote project" in your Graphcool account before its API gets updated:

1. You added a module that includes new type definitions as well as two serverless functions of type `resolver`.
2. You configured the data model with a new `Post` type and a relation to the `FirebaseUser` type from the module.

<Instruction>

*You can simply apply all the local changes using the following command in the terminal:*

```bash(path="graphcool")
graphcool deploy
```

</Instruction>

Here's what the generated output looks like. 

```(nocopy)
$ graphcool deploy
Deploying to project __PROJECT_ID__ with local environment dev.... ✔

Your project __PROJECT_ID__ of env dev was successfully updated.
Here are the changes:


Types

  Post
   + A new type with the name `Post` is created.
    ├── +  A new field with the name `description` and type `String!` is created.
    └── +  A new field with the name `imageUrl` and type `String!` is created.
  FirebaseUser
   + A new type with the name `FirebaseUser` is created.
    └── +  A new field with the name `firebaseUserId` and type `String` is created.

Relations

  PostsByUser
   + The relation `PostsByUser` is created. It connects the type `Post` with the type `FirebaseUser`.

Resolver Functions

  authenticateFirebaseUser
   + A new resolver function with the name `authenticateFirebaseUser` is created.
  loggedInUser
   + A new resolver function with the name `loggedInUser` is created.

RootTokens

  firebase-authentication
   + A rootToken with the name `firebase-authentication` is created.
```

This reflects precisely the changes we mentioned above.

> You can now open your project in a GraphQL Playground (using the `graphcool playground` command) and send queries and mutations. 

## Connecting the App with Firebase

The "Login with Firebase" authentication works in the way that your app will be receiving an "ID token" from the Firebase API that proves your users' identities. In order for this flow to work, you need to first create a _Firebase app_.

### Creating a Firebase app

<Instruction>

Got to the [Firebase Console](https://console.firebase.google.com) and click the `Add project` button. In the popup, set a name for the new project and click `CREATE PROJECT`.

</Instruction>


### Configuring Firebase in your app

#### Frontend

As a next step, you need to tell the Firebase authentication UI (which is already included in the project) about the new Firebase app you just created. You therefore need to copy your the configuration data of your new Firebase app (such as the API key, auth domain,...) into your project.

<Instruction>

Open to the `Overview` page of your Firebase app in the [Firebase Console](https://console.firebase.google.com).

Then click `Add Firebase to your web app`. 

From the resulting popup, copy over all the values for `apiKey`, `authDomain`, `databaseURL`, `projectId`, `storageBucket` and `messagingSenderId` into the corresponding fields of the `config` object inside `./src/firebase.js`.

![](https://imgur.com/xQ3WMtz.png)

</Instruction> 

#### Backend

Since you're also using the Firebase SDK in the backend to [verify your users' Firebase ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens), you also need to add some configuration there. 

<Instruction>

Again in the [Firebase Console](https://console.firebase.google.com), navigate to your project settings by clicking the little _Settings_-icon in the top-left. 

Then navigate to the `SERVICE ACCOUNTS`-tab and click on `GENERATE NEW PRIVATE KEY` on the bottom of the page.

![](https://imgur.com/qgi9Pmx.png)

</Instruction>

This will download a JSON file that contains some admin info about your app.

<Instruction>

Copy the full JSON object that's contained in the downloaded file as the value for `serviceAccount` inside `graphcool/modules/firebase/code/authenticateFirebaseUser.js`. 

</Instruction>

That's it - your app is now ready to make use of the Firebase login! 🎉


## Configuring Apollo Client

You're finally getting to the point where you can turn your attention towards the frontend.

The first thing you need to is add the dependencies for Apollo Client.

<Instruction>

*In the root directory of your project, add the following dependencies using [yarn](https://yarnpkg.com/en/):*

```bash(path="")
yarn add react-apollo
```

</Instruction>

Next you need to instantiate the `ApolloClient` and configure it with the endpoint of your GraphQL API.

<Instruction>

*Open `index.js` and add the following import statements to its top:*

```js(path="src/index.js")
import { ApolloClient, ApolloProvider, createNetworkInterface } from 'react-apollo'
```

Having the imports available, you can now instantiate the `ApolloClient`. Add the following code right below the import statements:

```js(path="src/index.js")
const networkInterface = createNetworkInterface({ uri: 'https://api.graph.cool/simple/v1/__PROJECT_ID__' })
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

The `ApolloClient` is your main interface to the GraphQL server and will take care of sending all queries and mutations for you. The last thing you need to do is replace the `__PROJECT_ID__` placeholder when calling `createNetworkInterface`. 

<Instruction>

*To get access to your project ID, simply use the following command in the terminal:*

```bash(path="graphcool")
graphcool info
```

Then copy the value for `projectId` and replace the `__PROJECT_ID__` placeholder from before.

</Instruction>


## Implementing Firebase authentication

### The "Login with Firebase" flow

Here's what's supposed to happen when the user wants to authenticate themselves with Firebase in the app:

1. The user clicks the **Login with Firebase** button
2. The [Firebase Auth UI](https://github.com/firebase/firebaseui-web) is loaded, the user selects their authentication method of choice (e.g. _Google Login_ or _Phone Authentication_)
3. The app receives a [Firebase ID Token](https://firebase.google.com/docs/auth/admin/verify-id-tokens) (inside `signInSuccess` callback in `components/FirebaseLogin.js`)
4. Your app calls the Graphcool mutation `authenticateFirebaseUser(firebaseIdToken: String!)`
5. If no user exists yet that corresponds to the passed Firebase ID token, a new `FirebaseUser` node will be created
6. In any case, the `authenticateFirebaseUser(firebaseIdToken: String!)` mutation returns a valid token for the user
7. Your app stores the token and stores it in `localStorage` where it can be accessed by `ApolloClient` and used to authenticate all subsequent requests

### Creating a new `FirebaseUser`

The first three steps are of the authentication flow are effectively taken care of by the Firebase SDK, it's now your task to add the required functionality on the Graphcool end. Step 4 stays you need to call the `authenticateFirebaseUser(firebaseIdToken: String!)` mutation with the token that your received from Firebase, so that's what you'll do next!

<Instruction>

*Open `FirebaseLogin.js` and add the following mutation to the bottom of the file, also replacing the current export statement:*

```js(path="src/components/FirebaseLogin.js)
const AUTHENTICATE_FIREBASE_USER = gql`
  mutation AuthenticateFirebaseUserMutation($firebaseIdToken: String!) {
    authenticateFirebaseUser(firebaseIdToken: $firebaseIdToken) {
      token
    }
  }
`

export default graphql(AUTHENTICATE_FIREBASE_USER, { name: 'authenticateFirebaseUserMutation' })(withRouter(App))
```

For this code to work you also need to add the following import to the top of the file:

```js(path="src/components/FirebaseLogin.js)
import { gql, graphql } from 'react-apollo'
```

</Instruction>

By using Apollo's higher-order component `graphql`, you're "combining" your React component with the `authenticateFirebaseUser`-mutation. Apollo will now inject a function called `authenticateFirebaseUserMutation` into the props of your component that will send the given mutation to the API for you.

The last thing you need to do to make the authentication flow work, is actually call that function.

<Instruction>

*Still in `FirebaseLogin.js`, adjust the implementation of the `signInSuccess` callback to look as follows:*

```js(path="src/components/App.js)
'signInSuccess': async currentUser => {

  const firebaseIdToken = await currentUser.getIdToken()
  const authenticateUserResult = await this.props.authenticateFirebaseUserMutation({
    variables: { firebaseIdToken }
  })

  if (authenticateUserResult.data.authenticateFirebaseUser.token) {
    localStorage.setItem('graphcoolToken', authenticateUserResult.data.authenticateFirebaseUser.token)
  } else {
    console.error(`No token received from Graphcool`)
  }

  self.props.history.replace('/')
  return false
}
```

</Instruction>

That's it, the Firebase authentication is now implemented. If you run the app, click the "Login with Firebase"-button and then continue the authentication flow either with Google Login or via Phone Authentication, a new `FirebaseUser` will be created in the database. You can verify that in the Graphcool console or a Playground.


## Checking the authenticated status

In the app, you want to be able to detect whether a user is currently logged in. A very simple way to do so would be to simply check whether `localStorage` contains a value for the key `graphcoolToken`, since that's how you're storing the authentication tokens after having received them from your API.

Notice however that these tokens are _temporary_, meaning they'll eventually expire and can't be used for authentication any more. This means that ideally you should not only check whether you currently have a token available in `localStorage`, but actually validate it _against the API_ to confirm that it's either valid or expired.

For exactly this purpose, the `firebase` module provides a dedicated query that you can send to the API, with an authentication token attached to the request, and the server will return the `id` of a logged-in user or `null` if the token is not valid.

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

*Open `App.js` and add the `loggedInUser` query to the `App` component, again by also replacing the current export statement:*

```js{}(path="src/components/App.js")
const LOGGED_IN_USER = gql`
  query LoggedInUser {
    loggedInUser {
      id
    }
  }
`

export default graphql(LOGGED_IN_USER, { options: { fetchPolicy: 'network-only'} })(withRouter(App))
```

To make this work you now need to also import the `gql` and `graphql` functions from the `react-apollo` package:

```js{}(path="src/components/App.js")
import { gql, graphql } from 'react-apollo'
```

</Instruction>

Whenever the `App` component now loads, Apollo executes the `loggedInUser` query against the API. So now you need to make sure that the result of the query is used accordingly to render the UI of your app. If the query was successful and returned the `id` of a logged-in user, you want to display a logout-button as well as a button for the user to create a new post. Otherwise, you simply render the same UI as before with the login- and signup-buttons.

Notice you're specifying the `fetchPolicy` when you're adding the `loggedInUser` query to the `App` component. This is to make sure that Apollo actually executes the request against the API rather than looking up a previous result in its cache.

<Instruction>

*To properly render the login-state of the user, first implement the `_isLoggedIn` function inside `App.js` function properly:*

```js{}(path="src/components/App.js")
_isLoggedIn = () => {
  const loggedIn = this.props.data.loggedInUser &&
    this.props.data.loggedInUser.id &&
    this.props.data.loggedInUser.id !== ''
  return loggedIn
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


Lastly, to also account for the ongoing network request, you should make sure to render a loading-state for the user.

<Instruction>

*Still in `App.js`, update `render` to look as follows:*

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

This is all the code you need to implement the logged-in status. However, when running the app you'll notice that despite the fact that you are already logged in (at least that's the case if you've created a new user before), the UI still looks as before and doesn't render neither the logout-button, nor the button for the user to create new posts.

That's because the token is not yet attached to the request, so your GraphQL server doesn't know in whose name the reuqest is sent! 

To attach the token to the request's header, you need to configure your `ApolloClient` instance accordingly, since it is responsible for sending all the HTTP requests that contain your queries and mutations.

<Instruction>

*Open `index.js` and add the following configuration right before you're instantiating the `ApolloClient`:*

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

*Open `ListPage.js` and add the following query along with the export statement to the bottom of the file:*

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

*Open `CreatePost.js` and add the following mutation to the bottom of the file, like before also replacing the current export statement:*

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

*In `CreatePost.js`, add the `loggedInUser` query to the bottom of the file (right before the export statement):*

```js(path="src/components/CreatePost.js")
const LOGGED_IN_USER = gql`
  query LoggedInUser {
    loggedInUser {
      id
    }
  }
`
```

Now, you can use Apollo's `compose` function to easily add multiple GraphQL operations to the component. Adjust the current export statement to look like this:

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

We also want to secure data access and configure permission rules such that only authenticated users can create new posts, and only the author of a post can update or delete it. Right now, _everyone_ who has access to the endpoint of your GraphQL server is able to perform these operations (despite the fact that the UI of your React app of doesn't allow this)!

> Note: Everyone who has access to the endpoint of your GraphQL server can simply access all your data, e.g. through a Playground that can be opened by pasting the endpoint into the address bar of a browser.

### Permissions in Graphcool

When using Graphcool, you need to explicitly allow your clients to perform the operations that are exposed by your API. But wait! If that's the case, why were you able to create and download posts before then? Shouldn't you have had to explicitly allow these operations then?

The reason why the `allPosts` query and `createPost` mutation were already working is simple. When a new Graphcool project is created, there is a _wildcard permission_ setup for you that does the job of allowing _all_ operations.

All permission rules need to be defined in the project definition, the `graphcool.yml`-file. If you check this file now, you'll only see one permission that's currently part of your project and that was initially added by the Graphcool CLI:

```yml(path="graphcool/graphcool.yml&nocopy")
permissions:
  - operation: '*'
```

This simply expresses that all operations are allowed.

> If you remove this one permission and run `graphcool deploy` afterwards, you'll notice that all your queries and operations will fail with a _Permission denied_ error.

Generally, the `permissions` property in the project definition contains a list of _permissions_. A single permission has the following properties:

- `operation` (required): Specifies for which operation (query or mutation) this operation applies
- `authenticated` (not required, default: `false`): Indicates whether a client who wants to perform this operation needs to be authenticated
- `query` (not required): Points to a file that contains a permission query that defines the rules for who is allowed to perform this operation.

<Instruction>

*To start with a clean slate, remove the `- operation: '*'` from the `graphcool.yml` file.*

</Instruction>

### Allowing all users to see posts

The first permission you're going to add is the one to allow everyone to see posts.

<Instruction>

*In the `graphcool.yml`-file, add the following permission under the `permissions`-section:*

```yml(path="graphcool/graphcool.yml")
- operation: Post.read
```

</Instruction>

### Allowing only authenticated users to create posts

For the next permission to allow only authenticated users to create new posts, you'll make use of the `authenticated` property.

<Instruction>

*Still in `graphcool.yml`, add the following permissions right below the previous one:*

```yml(path="graphcool/graphcool.yml")
- operation: Post.create
  authenticated: true
- operation: UsersPosts.connect
  authenticated: true
```

</Instruction>

This now expresses that users who are trying to perform the `createPost` mutation need to be authenticated. The second permission for `UsersPosts` is needed because the `createPost` mutation will also create a relation (which is called `UsersPosts` as defined in `types.graphql`), so the `connect` operation on the relation needs to be allowed as well.

### Allowing only the authors of a post to udpate and delete it

For these permissions, you need to make use a _permission query_. Permission queries are regular GraphQL queries that only return `true` or `false`. If you specify a permission query for an operation, the permission query will be executed right before the corresponding operation is request by a client. Only if the query then returns `true`, the operation will actually be performed, otherwise it fails with a permission error.

<Instruction>

*First, add the permissions to the project definition:*

```yml(path="graphcool/graphcool.yml")
- operation: Post.update
  authenticated: true
  query: permissions/updatePosts.graphql
- operation: Post.delete
  authenticated: true
  query: permissions/deletePosts.graphql
```

</Instruction>

Both permissions require the user to be authenticated, but they also point to files that contain more concrete rules for these operations. So, you'll need to create these files next!

<Instruction>

*Create a new directory inside `graphcool` and call it `permissions`.*

Then create two new files inside that new directory, call them `updatePosts.graphql` and `deletePosts.graphql`.

Finally, add the following permission query into _both_ files:

```graphql(path="graphcool/permissions/updatePosts.graphql")
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

Notice that the arguments `$node_id` and `$user_id` will be injected into the query automatically when it is executed. `$node_id` is the `id` of the post that is to be updated (or deleted), and `$user_id` is the `id` of the currently logged in user.

With that knowledge, you can derive the meaning of the permission query. It effecticely requires two things:

1. There needs to exist a post with its `id` being equal to `$node_id`
2. The `id` of the `author` of this post, needs to be equal to `$user_id`

Lastly, you only need to make sure the changes are applied to your project.

<Instruction>

*In the `graphcool` directory, execute the following command in the terminal:*

```bash(path="graphcool")
graphcool deploy
```

</Instruction>
 
Awesome! Now the permission rules apply and all our initial requirements for the app are fulfilled!

## Summary

In this guide, you learned how to build a simple app using an Firebase-based authentication workflow.

You created your GraphQL server from scratch using the Graphcool CLI and customized the [`firebase`](https://github.com/graphcool/modules/tree/master/authentication/firebase) authentication module according to your needs by adding a relation to the `Post` type.

You then configured Apollo Client inside your React app and implemented all required operations. Finally you removed the wildcard permission from the project and explicitly defined permission rules for the operations that your API exposes.


