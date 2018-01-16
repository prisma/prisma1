# GraphQL Server - Github Auth

This example illustrates the implemention of Github Authentication with GraphQL Server, using `graphql-yoga` and `prisma-biniding`.

## Getting started

### Initializing the Prisma Database service

```sh
prisma deploy # copy simple API endpoint into the `PRISMA_ENPOINT` env var in .env
```

### Setting up the Github OAuth2

You need to configure these credentials from a new Github OAuth2 app as environment variables:

* `GITHUB_CLIENT_ID`
* `GITHUB_CLIENT_SECRET`

1. Go to [github.com](github.com) and navigate to your profile. Click on your profile icon in the upper right corner and enter `Settings`.
2. In the lower left side find _Developer Settings_ and navigate to _OAuth Apps_.
3. Click `New OAuth App` and give your app a nice name. For the purposes of the example, it is best to set the _Homepage URL_ to `http://localhost:8000` and _Authorization callback URL_ to `http://localhost:8000/login`. (Application description is optional).
4. Register the application.
5. Copy _Client ID_ and _Client Secret_ to the __.env__ file.

#### Testing with WEB

* Replace `__CLIENT_ID__` in `login.html`
* Serve `login.html`, for example by using `python -m SimpleHTTPServer`
* Open `https://localhost:8000/login.html` in a browser, open the DevTools and authenticate with your Github account
* Copy the code printed in the Console of your DevTools

#### Testing with "simple hack"

In order to obtain `Github code` you can also use this little hack.

1. Navigate to `https://github.com/login/oauth/authorize?client_id={GITHUB_CLIENT_ID}&scope=user` and replace `{GITHUB_CLIENT_ID}` with your Github client ID.
2. Authorise access to the account and you will be redirected to `localhost:8000/login.html?code={GITHUB_CODE}`.
3. Copy the `{GITHUB_CODE}` part of `localhost:8000/login.html?code={GITHUB_CODE}` url to your GraphQL playground where you can test authentication.

#### Queries and Mutations
1. To authenticate the user use `Mutation authenticate`:
```gql
mutation LoginOrSignup {
    authenticate(githubCode: "mygithubcode") {
        token
        user {
            name
            notes
        }
    }
}
```
Every time `authenticate` is called user info is loaded from Github server using provided code. If code is valid, user id is compared against existing users. If no user with such id exists, new one is created, otherwise the existsing one is returned.

2. To get info about currently authenticated user use `Query me`:
```gql
query Me {
    me {
        name
        bio
        public_repos
        notes {
            id
            text
        }
    }
}
```
Server will use the token, provided under `Authorization: Bearer <token>` http header, to identify userId and will search the database for an existsing user.

3. To create a Note use `Mutation createNote`, this will create a note connected with your profile.
```gql
mutation NewNote {
    createNote(text: "Super cool text.") {
        id
        text
    }
}

query MyProfile {
    me {
        id
        notes { # <- Note will appear here
            id
            text
        }
    }
}
```

4. To read, delete or update Note you will have to be authenticated, otherwise __*NotAuthorized*__ Error will be returned.
```gql
query MyNote($id: ID!) {
    note(id: $id) { text }
}
```

### Starting the Server

```sh
yarn install
yarn start
# Open http://localhost:5000/
```

## License
MIT
