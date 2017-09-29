# email-password

Add email and password login to your Graphcool Project 🎁

## Getting Started

```sh
npm -g install graphcool
graphcool init
graphcool module add graphcool/modules/authentication/email-password
```

## Configuration

After downloading the module, add it to the `modules` section in your `graphcool.yml` file:

```yaml
modules:
  email-password: modules/email-password/graphcool.yml
```

## Flow

### Signup

1. Your app calls the Graphcool mutation `signupUser(email: String!, password: String!)`.
2. If no user exists yet that corresponds to the passed `email`, a new `User` node will be created with the password (after being hashed and salted).
3. If a user with the passed `email` exists, a `User` node is not created and an error is returned.
4. If a user is created, then the `signupUser(email: String!, password: String!)` mutation returns the id for the new user.

### Login

1. Your app calls the Graphcool mutation `authenticateUser(email: String!, password: String!)`.
2. If no user exists yet that corresponds to the passed `email`, or the `password` does not match, an error will be returned.
3. If a user with the passed `email` exists and the `password` matches, the mutation returns a valid token for the user.
4. Your app stores the token and uses it in its `Authorization` header for all further requests to Graphcool.

## Test the Code

Go to the Graphcool Playground:

```sh
graphcool playground
```

Run this mutation to create a user:

```graphql
mutation {
  # replace __EMAIL__ and __PASSWORD__
  signupUser(email: "__EMAIL__", password: "__PASSWORD__") {
    id
  }
}
```

and this to authenticate as that user:

```graphql
mutation {
  # replace __EMAIL__ and __PASSWORD__
  authenticateUser(email: "__EMAIL__", password: "__PASSWORD__") {
    token
  }
}
```

![](http://i.imgur.com/5RHR6Ku.png)
