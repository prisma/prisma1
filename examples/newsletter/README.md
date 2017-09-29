# newsletter

## Getting Started

```sh
npm install -g graphcool@beta
git clone git@github.com:graphcool-examples/graphcool-examples.git
cd graphcool-examples/newsletter
graphcool init
```

## Configuration

* Check the README for the `mailgun` module in `modules/mailgun/README.md` to setup mailgun.

* Add the `SENDER_EMAIL` environment variable. In the sandbox mode of mailgun, it needs to be the same as the email address you used to signed up to mailgun.

```
export SENDER_EMAIL=xxx
```

## Features

- Collect newsletter subscribers
- New subscribers will receive a welcome email
- WIP: Send out batched newsletter emails

## Test the Code

Go to the Graphcool Playground:

```sh
graphcool playground
```

Hook into the function logs:

```sh
graphcool logs -f welcomeEmail --tail
graphcool logs -f sendEmail --tail # in a different terminal
```

Run this mutation to create a new subscriber:

```graphql
mutation {
  # replace __EMAIL__ with the value of SENDER_EMAIL from above!
  createSubscriber(
    email: "__EMAIL__"
    firstName: "John"
    lastName: "Doe"
    isSubscribed: true
  ) {
    id
  }
}
```

A new welcome email should have been sent to the email you provided.

**Note that in the sandbox mode of mailgun, you can only send emails to and from the email you signed up.**
