---
alias: thohk5ovul
description: Server Hosting
---

# Server Hosting

In this section, you'll learn how you can host a GraphQL server that connects with your Graphcool data store.

## Background

With Graphcool 1.0, Graphcool only acts as a "database" for your GraphQL server. This is different from the Graphcool Framework and BaaS where Graphcool was responsible for _all_ server-side logic and other functionality.

It is recommended that you're using `graphql-yoga` as your GraphQL server.

<InfoBox type=info>

**IMPORTANT**: When hosting your GraphQL server (e.g. with Zeit Now, AWS Lambda or some other hosting provider), you should ensure that it is deployed to the same _region_ as your Graphcool service.

<InfoBox>

## Deployment with Zeit Now

Now is a "one-click-deployment" tool allowing to easily deploy a server to the web. Here is how it works:

* **Step 1 - Download Zeit Desktop**: Zeit provides a Desktop app to manage your deployments. There also is a CLI that comes along with the desktop app. You can download the app [here](https://zeit.co/download) or using homebrew. For information about installation using homebrew, check this [GitHub issue](https://github.com/zeit/now-cli/issues/688).
* **Step 2 - Deploy your GraphQL server**: To deploy your GraphQL server, all you need to is run the `now` command in the root directory of your web server.

Assume your GraphQL server has the following file structure:

```
myapp/
├── database
│   ├── datamodel.graphql
│   └── graphcool.yml
├── node_modules
├── package.json
└── src
    └── index.js
```

In that case, you can deploy web server like so:

```sh
cd /path/to/myapp
now
```

## Deployment with the Serverless Framework

Another option to deploy your GraphQL server is by simply using a serverless functions provider, such as AWS Lambda, Google Cloud or Microsoft Azure. The easiest way to do so is by using the [Serverless Framework](https://serverless.com/).

<InfoBox type=warning>

**Attention**: Deploying your GraphQL server using a serverless functions provider is only possible if your clients are not using GraphQL subscriptions for realtime functionality. The reason for that is that subscriptions require the web server to _maintain state_ (because the server needs to remember which clients are subscribed to which events and retain an open connection to these clients). This is not possible with serverless functions.

</InfoBox>

To get started with the Serverless Framework, you need to install the CLI and sign up:

```sh
npm install -g serverless
serverless login
```

Depending on your serverless functions provider, you can then follow instructions from the [quickstart documentation](https://serverless.com/framework/docs/getting-started/) of the Serverless Framework:

* [AWS Lambda](https://serverless.com/framework/docs/providers/aws/guide/quick-start/)
* [Microsoft Azure](https://serverless.com/framework/docs/providers/azure/guide/quick-start/)
* [IBM Open Whisk](https://serverless.com/framework/docs/providers/openwhisk/guide/quick-start/)
* [Google Cloud Platform](https://serverless.com/framework/docs/providers/google/guide/quick-start/)
* [Kubeless](https://serverless.com/framework/docs/providers/kubeless/guide/quick-start/)
* [Spotinst](https://serverless.com/framework/docs/providers/spotinst/guide/quick-start/)
* [Webtasks](https://serverless.com/framework/docs/providers/webtasks/guide/quick-start/)
