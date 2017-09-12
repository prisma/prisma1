---
alias: dah6aifoce
description: Use Zapier to create a Slack integration for your GraphQL server and get Slack notifications whenever a certain mutation is executed.
---

# Sending Slack Notifications with GraphQL & Zapier

[Slack](https://slack.com/) is very popular to handle the internal communication for teams. Slack notifications help to easily stay uptodate with things like successful or failing builds. In this guide we will focus on another use case: tracking business related events like when a new user signs up or a purchase is made.

We will use a [Server-Side Subscription](!alias-ahlohd8ohn) to trigger the Slack plugin offered by [Zapier](https://zapier.com/)'s webhook feature.

## 1. Preparation

We need to setup the individual parts before we can wire everything up.

### 1.1 Setting up Slack

You'll need a new Slack team you administrate - unless your team members don't mind a few notifications in the `random` channel... [Create a free Slack team](https://slack.com/create) and you're good!

### 1.2 Setting up Zapier

If you don't use Zapier yet, [sign up for free](https://zapier.com/sign-up/).

### 1.3 Setting up Graphcool

You can create a new Graphcool project so you can freely experiment. If you have no Graphcool account yet, [create a free account here](https://graph.cool).

Head over to your Graphcool project and add a new required `name` field of type String to the `User` type. We will print a user's name in the Slack channel whenever a new one signs up.

## 2. Creating a function to a Zapier webhook

Next we will add a new Server-Side Subscription function that calls a Zapier webhook. The Server-Side Subscription is triggered whenever a new user is created.

### 2.1 Creating a new Zapier webhook

Head over to your Zapier account and create a new Zap. Choose `Webhooks by Zapier` for the trigger and select `Catch Hook`. When you are being prompted to test the webhook, switch back to your Graphcool project to setup a new server-side subscription.

### 2.2 Setup the Function

* Create a new [Server-Side Subscription](!alias-ahlohd8ohn)
* Choose `User is created` as the trigger
* For the payload, enter this query:

    ```graphql
    subscription {
      User(filter: {
        mutation_in: [CREATED],
      }) {
        node {
          id
          name
        }
      }
    }
    ```

* Paste the webhook url obtained from Zapier into the handler url field.

To progress further with Zapier, we will execute a test mutation.

### 2.3 Run test mutation

Run this mutation in your Graphcool playground for the Simple API:

```graphql
mutation {
  createUser(name: "Nilan") {
    id
  }
}
```

Back in your new Zap trigger, you should shortly see the incoming data. After you confirmed that everything is correct, now you can move on to creating the Slack notification action

### 2.4 Creating a Zapier action

As the Zapier action, we can now add a new Slack notification. Choose the `Slack` action and select `Send Channel Message`. Link your Slack account to your Zapier account and you should be good to go. Pick your channel, and create a message like `User <name> signed up (<id>)` where `<name>` and `<id>` have to be replaced by selecting the fields provided by Zapier.

## 3. Invoking Functions

At this point you are good to go - if you want, you can run the mutation from above again (you can be creative and use your name this time!) and confirm that the Slack notification appears.

## 4. Next Steps

You just setup a function that eventually notifies you whenever a new user signs up, neat!

If you want to read more about functions, head over to the [reference documentation](!alias-boo6uteemo).
