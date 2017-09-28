# github-authentication

Add Github Authentication to your Graphcool project 🎁

## Getting Started

```sh
npm -g install graphcool
graphcool init
graphcool module add graphcool/modules/authentication/github
```

## Configuration

In your base project, you need to configure the following **environment variables**.

- `CLIENT_ID`: Github Client ID
- `CLIENT_SECRET`: Github Client Secret


An easy way to set these up is using [direnv](https://direnv.net/).
To use `direnv`, put the following into `.envrc` in you project root:

```sh
export CLIENT_ID=xxx
export CLIENT_SECRET=xxx
```

Read on to see how to setup a Github App to obtain the environment variables.

## Authentication flow in app

1. The user clicks the `Authenticate with Github` button
2. The Github UI is loaded and the user accepts
3. The app receives a Github code
4. Your app calls the Graphcool mutation `authenticateGithubUser(githubCode: String!)`
5. If no user exists yet that corresponds to the passed `githubCode`, a new `GithubUser` node will be created
6. In any case, the `authenticateGithubUser(githubCode: String!)` mutation returns a valid token for the user
7. Your app stores the token and uses it in its `Authorization` header for all further requests to Graphcool

## Setup

### Create a Github App

To use Github Login you need to create a Github app and add the `Github Login` product. Follow [this guide to create an app](https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/registering-oauth-apps/) in a few minutes.

Once you created a new APP, add the and copy its App ID. Replace `__CLIENT_ID__` in `login.html` with your Github client ID.

![](docs/client-id.png)

Add `http://localhost:8000/login.html` to **Authorization callback URL**

![](docs/github-login-settings.png)

To create a test Github code, run `login.html`, for example using Python's `SimpleHTTPServer`:

```sh
python -m SimpleHTTPServer
```

Open `http://localhost:8000/login.html` in your browser and use the login button to authorize the app:

![](docs/authorize.png)

The Github code will be logged to the console.

## Test the Code

First, obtain a valid Github code with the small app in `login.html` as mentioned above.

Go to the Graphcool Playground:

```sh
graphcool playground
```

Run this mutation to authenticate a user:

```graphql
mutation {
  # replace __GITHUB_CODE__!
  authenticateGithubUser(githubCode: "__GITHUB_CODE__") {
    token
  }
}
```

You should see that a new user has been created. The returned token can be used to authenticate requests to your Graphcool API as that user. Note that running the mutation again with the same Github code will not add a new user.

## Contributions

Thanks so much [@katopz](https://github.com/katopz) for contributing this example :tada:

![](http://i.imgur.com/5RHR6Ku.png)
