# mailgun

Send emails with mailgun in your Graphcool project 🎁

## Getting Started

```sh
npm -g install graphcool
graphcool init
graphcool module add graphcool/modules/messaging/mailgun
```

## Configuration

In your base project, you need to configure the following **environment variables**.

- `MAILGUN_API_KEY`: mailgun API Key
- `MAILGUN_DOMAIN`: mailgun domain

You can receive them after [signing up at mailgun](https://app.mailgun.com/app/dashboard).

An easy way to setup environment variables is using [direnv](https://direnv.net/).
To use `direnv`, put the following into `.envrc` in you project root:

```sh
export MAILGUN_API_KEY=xxx
export MAILGUN_DOMAIN=xxx
```

## Flow

Use the `sendMailgunEmail` mutation to send emails according to its parameters:

* `tag: String!`: custom tag for internal logging
* `from: String!`: sender email
* `to: [String!]!`: a list of recipient emails
* `subject: String!`: the email subject, can contain references to `recipientVariables`
* `text: String!`: the email body, can contain references to `recipientVariables`
* `recipientVariables: Json`: optional recipient variables for [batched emails](http://mg-documentation.readthedocs.io/en/latest/user_manual.html#batch-sending). Read the documentation for more information on the encoding.

## Test the Code

Go to the Graphcool Playground:

```sh
graphcool playground
```

Hook into the function logs:

```sh
graphcool logs -f sendEmail --tail
```

Run this mutation to send a single email:

```graphql
mutation {
  # replace __SENDER_EMAIL__ and __RECIPIENT_EMAIL__ with *authorized* email addresses!
  sendMailgunEmail(
    tag: "2017-09-16-welcome-email"
    from: "__SENDER_EMAIL__"
    to: "__RECIPIENT_EMAIL__"
    subject: "A new email from the Graphcool mailgun module!"
    text: "This is your first email from the Graphcool mailgun module!"
  ) {
    success
  }
}
```

Run this mutation to send a batched email:

```graphql
mutation {
    # replace __SENDER_EMAIL__, __FIRST_EMAIL__ and __SECOND_EMAIL__ with *authorized* email addresses!
  createMailgunEmail(
    tag: "2017-09-16-batched-welcome-email"
    from: "__SENDER_EMAIL__"
    to: ["__FIRST_EMAIL__", "__SECOND_EMAIL__"]
    subject: "A new email from the Graphcool mailgun module!"
    text: "Hey %recipient.name%, this is your first email from the Graphcool mailgun module!"
    recipientVariables: "{\"__FIRST_EMAIL__\": {\"name\": \"First\"}, \"__SECOND_EMAIL__\": {\"name\": \"Second\"}}"

  ) {
    success
  }
}
```

![](http://i.imgur.com/5RHR6Ku.png)
