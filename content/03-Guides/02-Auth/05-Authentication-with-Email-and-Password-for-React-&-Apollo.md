---
alias: oopheesaj9
description: Learn how to provide secure data access to your GraphQL backend and provide email/password-based user authentication in React and Apollo.
---

# User Authentication with Email for React and Apollo

In this guide we will have a closer look at how auth providers and the permission system tie together when using a Graphcool project.
You will learn how to configure email authentication in combination with React and Apollo to handle user login and sign up and what permission setup is needed to get started with authentication.

In this Instagram clone, everyone will be able to see posts but only authenticated users should be able to create new posts. When a new user signs up, he has to enter his email, password and name and state if he is interested in notification emails.

> You can find the complete [example on GitHub](https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-email-and-apollo).


## 1. Setting up the Graphcool project

### 1.1 Creating the Project

The app will use the following data model:

```graphql
type Post {
  description: String!
  imageUrl: String!
}

type User {
  name: String!
  email: String!
  emailSubscription: Boolean!
}
```

You can create the project using the [Graphcool CLI](https://www.npmjs.com/package/graphcool):

```sh
# Install the Graphcool CLI
npm install -g graphcool

# Create new project
graphcool init --schema https://graphqlbin.com/insta-email.graphql
```


### 1.2 Setting the permissions

To make our application behave correctly we have to setup permissions for the `Post` type in our project.

As we want to restrict the creation of posts only to _authenticated_ users, we have to create the according permission for `CREATE` on the `Post` type.

![](./create-post-permissions.png?width=500)


### 1.3 Enabling email authentication

In the [console](https://console.graph.cool), open the **Integrations** tab in the side-menu and click on the **Email-Password Auth** integration. Then simply click **Enable** in the popup.


## 2. Building the application

That's it, we are done configuring the project and we can start working on our frontend application! Let's have a closer look at how we can use email authentication in React.

### 2.1 Setting up Apollo Client for user authentication

We will use [Apollo Client](http://dev.apollodata.com/) to make GraphQL requests in our React application.
In Graphcool, requests are authenticated using the `Authorization` header. That's why we include it in every request if the user already signed in. We can use `applyMiddleware` on the `networkInterface` to accomplish this:

```js
// in src/index.js
const networkInterface = createNetworkInterface({ uri: 'https://api.graph.cool/simple/v1/__PROJECT_ID__' })

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

Now we can create the Apollo Client instance `client` and pass it to `ApolloProvider` which exposes its functionality to all the React components we use. We also set up our routes using `react-router`.

```js
// in src/index.js
const client = new ApolloClient({ networkInterface })

ReactDOM.render((
  <ApolloProvider client={client}>
    <Router history={browserHistory}>
      <Route path='/' component={App} />
      <Route path='create' component={CreatePost} />
      <Route path='login' component={LoginUser} />
      <Route path='signup' component={CreateUser} />
    </Router>
  </ApolloProvider>
  ),
  document.getElementById('root')
)
```

As you can see, we have four routes:

* `/`, where we render our main app. Here the user can see a list of posts and login. A logged in user can logout and create new posts here.

* `create`, where the user can create new posts if logged in

* `login`, where an existing user can log in. To log in, a user has to provide his email and password.

* `signup`, where a new user can sign up. To sign up, a user has to provide his credentials as well as a name and can decide to get email notifications.

### 2.2 Handling authenticated status

Even though we are sending a token in every request after the user signed in, it might be not valid, or a user might tamper with the actual token in the local storage. To check if a request is actually authenticated, we use the `user` query in our main component, `App.js`:

```js
// in src/components/App.js
//...
const userQuery = gql`
  query {
    user {
      id
      name
    }
  }
`

export default graphql(userQuery, { options: {fetchPolicy: 'network-only' }})(withRouter(App))
```

We set the option `fetchPolicy` to `network-only` to make sure we are querying the user every time from the server and don't use Apollo's cache for this particular query. We can use the user query to determine the rendering of our main app:

```js
// in src/components/App.js
//...
_isLoggedIn = () => {
  return this.props.data.user
}
//...
```

To log out, we can simply remove the authentication token from local storage and reload to cleanly reset Apollo Client's cache:

```js
// in src/components/App.js
//...
_logout = () => {
  // remove token from local storage and reload page to reset apollo client
  window.localStorage.removeItem('graphcoolToken')
  window.location.reload()
}
//...
```

We use the logged in status to either render a login button or a logout button and a new post button in case the user is already logged in.

There are a few reasons we are force fetching the user in every request. For example, a user might be logged in for a long time already, and the authentication token already expired. If you make another query or mutation and included an invalid authentication token for whatever reason, you will get an error message. You could add additional logic to your application that identifies this situation with the help of the returned error and prompts the user to login again when the token expired. In our example we will simply redirect if the user query doesn't return what we expect.

We will use the user query to check if the user is authenticated in a few other components as well.

Now we can think about the component that gets rendered for the `/signup` where we use the `createUser` and `signinUser` mutations to first create a new user and then sign him in.

### 2.4 Handling user login

For the route `/signup` we use the `CreateUser` component. We need to supply the `createUser` and `signinUser` mutations and the `user` query to the inner component using the `graphql` helper method of `react-apollo`:

```js
// in src/components/CreateUser.js
const createUser = gql`
  mutation ($email: String!, $password: String!, $name: String!, $emailSubscription: Boolean!) {
    createUser(authProvider: {email: {email: $email, password: $password}}, name: $name, emailSubscription: $emailSubscription) {
      id
    }
  }
`

const signinUser = gql`
  mutation ($email: String!, $password: String!) {
    signinUser(email: {email: $email, password: $password}) {
      token
    }
  }
`

const userQuery = gql`
  query {
    user {
      id
    }
  }
`

export default graphql(createUser, {name: 'createUser'})(
  graphql(userQuery, { options: { fetchPolicy: 'network-only' }})(
    graphql(signinUser, {name: 'signinUser'})(
      withRouter(CreateUser))
    )
)
```

Note that we again use the query option `fetchPolicy` to ensure that the query is actually sent to the GraphQL server, instead of using a potential cached version of the user.

Then we can focus on the `render` method of the `CreateUser` component. Here, we make a few checks to verify that

* the `data` from Apollo has already finished loading

* there is no logged in user yet

```js
// in src/components/CreateUser.js
//...
render () {
  if (this.props.data.loading) {
    return (<div>Loading</div>)
  }

  // redirect if user is logged in
  if (this.props.data.user) {
    console.warn('already logged in')
    this.props.router.replace('/')
  }
// ...
}
```

If data has finished loading and no user is already logged in, we render the input elements needed for the sign up form. If the user entered the needed information, we additionally render a button to finish the sign up:

```js
// in src/components/CreateUser.js
render () {
// ...
  {this.state.name && this.state.email && this.state.password &&
  <button className='pa3 bg-black-10 bn dim ttu pointer' onClick={this.createUser}>Log in</button>
  }
//...
}
```

When the user clicks the button, we call the `createUser` method where we first collect the needed variables and then execute the `createUser` mutation:

```js
// in src/components/CreateUser.js
createUser = () => {
  const {email, password, name, emailSubscription} = this.state

  this.props.createUser({variables: {email, password, name, emailSubscription}})
    .then((response) => {
      this.props.signinUser({variables: {email, password}})
        .then((response) => {
          window.localStorage.setItem('graphcoolToken', response.data.signinUser.token)
          this.props.router.replace('/')
        }).catch((e) => {
          console.error(e)
          this.props.router.replace('/')
        })
    }).catch((e) => {
      console.error(e)
      this.props.router.replace('/')
    })
}
```

If the `createUser` mutation succeeded, we are signing in the user right away and save the authentication token obtained as a response from the `signinMutation` in local storage to authenticate further requests as discussed above.

After the successful mutations (or an error), we redirect the user back to `/` where he now can see the `+ New Post` button.

### 2.5 Handling user login

For the route `/login` we use the `LoginUser` component. Here we only need to supply the `signinUser` mutation and the `user` query similar to before:

```js
const signinUser = gql`
  mutation ($email: String!, $password: String!) {
    signinUser(email: {email: $email, password: $password}) {
      token
    }
  }
`

const userQuery = gql`
  query {
    user {
      id
    }
  }
`

export default graphql(signinUser, {name: 'signinUser'})(
  graphql(userQuery, { options: { fetchPolicy: 'network-only' }})(withRouter(CreateLogin))
)
```

We also wait for the data to load and redirect to '/' if a user is already logged in, as we did in the `CreateUser` component. If the user supplied the needed information, we render a `Log in` button that executes the `signinUser` mutation when clicked:

```js
signinUser = () => {
  const {email, password} = this.state

  this.props.signinUser({variables: {email, password}})
    .then((response) => {
      window.localStorage.setItem('graphcoolToken', response.data.signinUser.token)
      this.props.router.replace('/')
    }).catch((e) => {
      console.error(e)
      this.props.router.replace('/')
    })
}
```

After the successful mutation (or an error), we redirect the user back to `/` where he now can see the `+ New Post` button.

### 2.6 Creating new posts

Finally, let's have a look at the component `CreatePost` where a signed in user can create new posts. Again, we are wrapping the inner component with `graphql` from `react-apollo`:

```js
const createPost = gql`
  mutation ($description: String!, $imageUrl: String!){
    createPost(description: $description, imageUrl: $imageUrl) {
      id
    }
  }
`

const userQuery = gql`
  query {
    user {
      id
    }
  }
`

export default graphql(createPost)(
  graphql(userQuery, { options: { fetchPolicy: 'network-only' }} )(withRouter(CreatePost))
)
```

Again, we are using `fetchPolicy` for the user query to ensure we don't use Apollo's cache for this query. We use this query again to check if the user is signed in or not and redirect to `/` if the user is not signed in:

```js
// in src/components/CreatePost.js
render () {
  //...
  if (!this.props.data.user) {
    console.warn('only logged in users can create new posts')
    this.props.router.replace('/')
  }
  //...
}
```

If the user is signed in and once the data has finished loading, we render two text inputs for the description and image url and a button to create the new post.

## 3. Try it out yourself

If you want to run the example on your own and experiment a bit with the a application yourself, checkout the code at [GitHub](https://github.com/graphcool-examples/react-graphql/tree/master/authentication-with-email-and-apollo). If you've already setup the data schema and email-based authentication for your project, you only have to inject your simple API endpoint in `src/index.js` and run `yarn && yarn start` and you're good to go!

Make sure to try things that shouldn't be allowed, like visiting the `/create` route without being logged in or using a fake `graphcoolToken` in local storage. Have fun!

## 4. Next steps

Great, you set up everything that's needed for authentication in your Graphcool project, built a React application that uses Apollo to let user sign up or login and let them see or create posts.

Have a look at other [advanced features](https://blog.graph.cool/exploring-graphcool-the-serverless-graphql-backend-50637f7e5921).
