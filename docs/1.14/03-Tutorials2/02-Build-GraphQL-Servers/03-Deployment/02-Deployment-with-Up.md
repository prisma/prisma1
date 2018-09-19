---
alias: shieghae3d
description: Learn how to deploy your GraphQL server with Zeit Now.
---

# Deploying GraphQL Servers with Apex Up

The *lifecycle* of an application consists of several phases. Depending on your process, you’ll find *at least* two of them: A *development* and a *production* phase. In this tutorial, you will learn how to take an app from development to production by *deploying* it with the one-click deployment tool [Apex Up](https://github.com/apex/up).

![Apex Up makes it easy to deploy your GraphQL servers to [AWS Lambda](https://aws.amazon.com/lambda)](https://cdn-images-1.medium.com/max/2400/1*VhC4wGAaTpTP82EQSVpSWg.png)*Apex Up makes it easy to deploy your GraphQL servers to [AWS Lambda](https://aws.amazon.com/lambda)*

This tutorial has two parts:

1. **Basic**: Learn how to do a *simple and straightforward deployment* with Up
1. **Advanced**: Learn how to deploy with Up *with environment variables*

## From development to production

During development, you’ll usually test your software *locally*. When you’re working on a web app, this means you’re running a local server and access your application on http://localhost:4000 (or some other port).

*http://localhost* however is not a domain that people on different machines than yours will be able to access over the web. That’s why you need to *deploy* the application.

Just few years ago, deployment was an extremely cumbersome process where you needed to get access to some physical server and do an awful lot of manual configuration work. Today, all this nitty-gritty is abstracted away by great services like Apex Up or Zeit Now.

## One-click deployment with Apex Up

### Up is a simple interface to AWS Lambda

Apex Up is an awesome one-click deployment tool for [AWS Lambda](https://aws.amazon.com/lambda/). Lambda itself is a great service for deploying serverless functions and even entire web servers — but it doesn’t have the best developer experience around it. Up provides a simple and intuitive interface to Lambda so you don’t have to deal with the tedious AWS tooling.

### Installing Up

The easiest way to get started with Up is by following the quickstart instructions in the [README](https://github.com/apex/up#quick-start). 

<Instruction>

You can install it by running the following command in your terminal:

```bash
curl -sf https://up.apex.sh/install | sh
```

</Instruction>

### Deploying with Up

When deploying an application with Up, all you need to do is invoke the terminal command `up` inside the root directory of your project. This assumes the presence of a file called `up.json` inside the same directory — if there is none, the `up` CLI will prompt you a few questions right inside the terminal and create it for you.

## Prerequisite: AWS Credentials

Because Up uses AWS Lambda under the hood, you need to be able to authenticate with AWS when using it. This authentication is done by storing the AWS credentials on your machine.

If you have used the AWS CLI before, you likely already have credentials being stored on your machine. 

<Instruction>

Otherwise, you need to install the [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html) and create your credentials with it (note that in order to install it, you need to have Python and [pip](https://pypi.python.org/pypi/pip) installed on your machine):

```bash
pip install awscli --upgrade --user
```

</Instruction>

> **Note** The `--upgrade option` tells `pip` to upgrade any requirements that are already installed. The `--user` option tells `pip` to install the program to a subdirectory of your user directory to avoid modifying libraries used by your operating system. You can find more info [here](https://docs.aws.amazon.com/cli/latest/userguide/installing.html).

Once the AWS CLI is installed, you can go ahead and create your credentials. This requires you to have an AWS account and provide an ***AWS Access Key ID*** as well as the belonging ***AWS Secret Access Key***. From the [AWS documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html), here is how you get access to these:

1. Open the [IAM console](https://console.aws.amazon.com/iam/home?#home).
1. In the navigation pane of the console, choose **Users**.
1. Choose your IAM user name (not the check box).
1. Choose the **Security credentials** tab and then choose **Create access key**.
1. To see the new access key, choose **Show**. Your credentials will look something like this: `AKIAIOSFODNN7EXAMPLE` (*Access Key ID*) and `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` (*Secret Access Key*)
1. To download the key pair, choose **Download .csv file**. Store the keys in a secure location.
1. Keep the keys confidential in order to protect your AWS account, and never email them. Do not share them outside your organization, even if an inquiry appears to come from AWS or Amazon.com. No one who legitimately represents Amazon will ever ask you for your secret key.

<Instruction>

Once you have the *Access Key ID* and the *Secret Access Key* accessible, run the following command and provide them when prompted:

```bash
aws configure
```

</Instruction>

> If you need more help with setting up the credentials, you can check the corresponding section in the [Up documentation](https://up.docs.apex.sh/#aws_credentials.aws_credential_profiles) or [AWS](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) directly.

The end result will be that you now have a hidden `.aws` directory inside your home folder. In there, you’ll find a file called `credentials` looking similar to this:

```
[default]
aws_access_key_id = xxxxxxxx
aws_secret_access_key = xxxxxxxxxxxxxxxxxxxxxxxx
```

## Part 1: Basic Deployment with Up

### Bootstrap the GraphQL server

In this tutorial, you'll use the [`node-basic`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic) GraphQL boilerplate project as a sample server to be deployed. The easiest way to get access to this boilerplate is by using the `graphql create` command from the [GraphQL CLI](https://github.com/graphql-cli/graphql-cli/).

The boilerplate project is based on [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/), a lightweight GraphQL server based on Express.js, `apollo-server` and `graphql-tools`.

<Instruction>

If you haven't already, go ahead and install the GraphQL CLI. Then, bootstrap your GraphQL server with `graphql create`:

```bash
npm install -g graphql-cli
graphql create hello-basic --boilerplate node-basic
```

</Instruction>

<Instruction>

When prompted where (i.e. to which [Prisma server](!alias-eu2ood0she)) to deploy your Prisma service, choose the **Demo server** option. Note that this requires you to authenticate with [Prisma Cloud](https://www.prisma.io/cloud/) as that's where the Demo server is hosted.

</Instruction>

The above `graphql create` command creates a new directory called `hello-basic` where it places the source files for your GraphQL server as well as the configuration for the belonging Prisma service.

You now have a proper foundation to deploy your GraphQL server.

### Deploy the server with `up`

When deploying a web server with Up, all you need to do is navigate into the root directory of that server and invoke the `up` command. As mentioned above, this command either expects the presence of an already created `up.json` file or will create one for you if it doesn’t exist yet.

<Instruction>

Navigate into the newly created `hello-basic` directory and deploy the server:

```bash
cd hello-basic
up
```

Provide the required information when prompted by `up`:

* **Project name**: You can use the default `hello-basic` for this
* **AWS Profile**: If you have setup your credentials correctly, `up` will provide a list of profiles to choose from. If this doesn’t work, go back to the [previous section](prerequisite-aws-credentials) and properly configure your credentials.
* **AWS Region**: In order to get the best possible performance for your server, be sure to deploy the GraphQL server to the same AWS region as the Prisma service. If you chose `demo-eu1` as the target demo server for your Prisma API, select the _EU (Ireland)_ region — in case you chose `demo-us1`, select _US West (Oregon)_.

</Instruction>

After you’ve provided the required information, `up` will take the files inside the project directory and deploy them to Lambda (note that this can take a few minutes).

![](https://cdn-images-1.medium.com/max/2000/1*SyGokC5RADoRaBjoqFUhsA.png)

On Lambda, the server will be started by invoking the `start` script inside `package.json` (basically it will just use `yarn start` or `npm run start`). The `start` script looks as follows: `node src/index.js`.

All right, congratulations! The GraphQL server is deployed — so how can you access it? Just run the following command in the terminal:

```bash
up url --open
```

This opens a browser and points it to the URL where the GraphQL server was deployed (when omitting the `--open` flag, the URL just gets printed).

The browser shows a [GraphQL Playground](https://github.com/graphcool/graphql-playground) you can use to interact with your GraphQL server! There’s still a minor problem however: The schema is not yet loaded and you won’t be able to send any actual queries to the API.

This is simply due to a small [bug](https://github.com/graphcool/graphql-playground/issues/598) in the Playground. As you might notice, the URLs in the browser’s address bar and the one from the Playground’s address bar are different — the latter one lacks the URL’s *path* (`“/staging”`). Thus, all you need to do is manually append `/staging` to the Playground’s URL and hit the little *reload*-button on the right of the bar:

![](https://cdn-images-1.medium.com/max/2780/1*MEtPJM2ns2iKnZ0YjGEYIA.png)

Once you did that, you will be able to send queries and mutation to the API. For example, try the following query:

```graphql
query {
  drafts {
    id
    title
    text
    isPublished
  }
}
```

### Multi-stage deployment with Up

The `/staging` path that’s appended to the URL indicates the *stage* of the deployment. Up supports two stages: `/staging` (default) and `/production`.

To deploy to the `/production` stage, you can simply `run up production`.

## Part 2: Advanced Deployment with Up

### Use environment variables to configure deployment stages

In this part, you’ll learn how to configure *environment variables* when deploying with Up. Environment variables are a best practice to quickly switch between different *deployment stages* of your server.

For example, during development you’ll commonly want to run your server in a *development* environment. The development environment uses a different database than your *production* environment. This ensures you’re not accidentally messing with actual user data when developing a new feature.

Only once you’re confident the new feature works as expected, you’ll deploy it to *production*. Environment variables are a great way to distinguish between environments by pointing to a specific database.

Another common use case for environment variables is to provision *secrets* to the build environment. It’s likely that your database requires some sort of authentication, or maybe you’re using 3rd party services that require to provide API keys so you can access them. This kind of secret information is also commonly provided through environment variables.

### Bootstrap the GraphQL server

Similar to before, you’ll start by bootstrapping a GraphQL server.

<Instruction>

This time, you’re going to use the [node-advanced](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/advanced) GraphQL boilerplate project:

```bash
graphql create hello-advanced --boilerplate node-advanced
```

</Instruction>

<Instruction>

Just like before, deploy the Prisma service to the Demo server in Prisma Cloud.

</Instruction>

The three core differences to the `node-basic` boilerplate are the following:

* `node-advanced` comes with built-in user authentication
* `node-advanced` uses environment variables (via `.env` files)
* `node-advanced` has realtime functionality (GraphQL Subscriptions)

**ATTENTION:** One thing that is really important to realize when deploying GraphQL servers to AWS Lambda is that realtime functionality based on GraphQL subscriptions do not work!

The reason for that is that subscriptions are implemented with WebSockets and the server needs to maintain a pool of connections to the subscribed clients — the server is hence *stateful*. However, AWS Lambda does not support stateful apps! Lambda works in the way that it provisions infrastructure for the servers on-the-go and tears it down when not needed any more. The teardown breaks any connections from previously subscribed clients!

### Accessing environment variables

Before moving on, consider these following two files:

**src/index.js**

```js
const server = new GraphQLServer({
  typeDefs: 'src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: process.env.PRISMA_ENDPOINT,
      debug: true,
    }),
  }),
})
```

**database/prisma.yml**

```yml
# The endpoint of your Prisma API (deployed to a Prisma Sandbox).
endpoint: ${env:PRISMA_ENDPOINT}

# The file containing the definition of your data model.
datamodel: datamodel.graphql

# Seed your service with initial data based on `seed.graphql`.
seed:
  import: seed.graphql

# Download the GraphQL schema of the Prisma API into 
# `src/generated/prisma.graphql` (as specfied in `.graphqlconfig.yml`).
hooks:
  post-deploy:
    - graphql get-schema --project database

# If specified, the `secret` must be used to generate a JWT which is attached
# to the `Authorization` header of HTTP requests made against the Prisma API.
# Info: https://www.prisma.io/docs/reference/prisma-api/concepts-utee3eiquo#authentication
# secret: ${env:PRISMA_SECRET}
```

In `index.js`, one environment variable is referenced when instantiating the `Prisma` binding object. With `process.env.PRISMA_ENDPOINT`, the code expects that an environment variable called `PRISMA_ENDPOINT` is set. (Note that the same holds true for `PRISMA_SECRET` if the comments for it are removed from `index.js` and `prisma.yml`).

Similarly, `prisma.yml` provides the values for three of its five properties using environment variables. In addition to `PRISMA_SECRET`, it also references a `PRISMA_ENDPOINT`.

The most obvious and straightforward way to provide these values now would be to set them directly in your shell,. e.g. with the following command:

```bash
export PRISMA_ENDPOINT="https://eu1.prisma.sh/jane-doe/hello-advanced/dev"
```

However, this approach is often not practical (or at all *feasible*) when working with state-of-the-art deployment and [continuous integration](https://en.wikipedia.org/wiki/Continuous_integration) tools, such as [CircleCI](https://circleci.com), [Buildkite](https://buildkite.com/), Zeit Now or Apex Up. This is because you don’t have direct access to the build environment — you cannot just [ssh](https://en.wikipedia.org/wiki/Secure_Shell) into the machine that’s going to build and start your server in order to set environment variables. Instead these tools provide you with custom mechanisms for setting environment variables, or follow best practices for doing so.

In the case of Up, environment variables can be set by adding an `environment` object to `up.json`. Note that [Up Pro](https://github.com/apex/up#pro-features) also offers an additional [up env](https://up.docs.apex.sh/#commands.env) command to encrypt and manage environment variables.

The `node-advanced` boilerplate is configured to use `.env` files in combination with [dotenv](https://github.com/motdotla/dotenv) by default.

<Instruction>

Since `.env` files are not supported by Up, you’ll have to manually copy over the values into the mentioned `environment` object inside `up.json`:

```json
{
  "name": "nodeadvanced-up",
  "profile": "default",
  "regions": ["eu-west-1"],
  "environment": {
    "PRISMA_ENDPOINT": "https://eu1.prisma.sh/jane-doe/hello-advanced/dev",
    "APP_SECRET": "jwtsecret123"
  }
}
```

</Instruction>

Notice that the `PRISMA_ENDPOINT` will look slightly different for you.

<Instruction>

That’s it — you can now go ahead and deploy the project:

```bash
up
```

</Instruction>

<Instruction>

Then open it in the browser with:

```bash
up url --open
```

</Instruction>

To test the app, you can send the same query from above again. To see what other operations are available, check out the interactive API documentation right inside the Playground.

## Summary

In this tutorial, you learned how to deploy a GraphQL server with [Apex Up](https://github.com/apex/up), an awesome one-click deployment tool. As sample GraphQL servers to deploy, you used the node-basic and node-advanced [GraphQL boilerplate](https://github.com/graphql-boilerplates) projects.

Deploying `node-basic` was straightforward since all required configuration was hardcoded in the project. For the `node-advanced` project, you used environment variables that you configured by using the custom mechanism provided by Up.
