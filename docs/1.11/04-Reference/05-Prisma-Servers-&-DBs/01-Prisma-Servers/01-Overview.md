---
alias: eu2ood0she
description: Overview
---

# Overview

Prisma APIs are hosted on _Prisma servers_. These servers provide the _runtime environment_ for Prisma APIs.

> **Note**: Prior to Prisma 1.7, Prisma servers have been called [_clusters_](!alias-iquaecuj6b#terminology).

In essence, there are three kinds of _servers_ you can deploy your Prisma API to:

- **Local / self-hosted** (using [Docker](https://www.docker.com/)): You can spin up your own Prisma server locally or host them using a cloud provider of your choice. They are managed with the [Docker CLI](https://docs.docker.com/engine/reference/commandline/cli/) which governs the underlying Docker _images_ and _containers_ for you. Follow [this](!alias-texoo9aemu) tutorial to learn how to host your own Prisma server on Digital Ocean.
- **Prisma Sandbox** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): Prisma Cloud offers free development servers which you can use for learning and prototyping, these servers are called **Prisma Sandbox**. Note that when deployed to a Prisma Sandbox, your APIs will be rate limited and have an upper bound in storage capacity (see the info box below for further info).
- **Private servers** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): A private server is connected to your own database which you're provisioning when initially setting up the server.

For production use cases, **private and self-hosted Prisma servers are the preferred option to deploy Prisma services**. This chapter is about self-hosted servers, you can learn more about Prisma Cloud [here](!alias-fae2ooth2u).

<InfoBox>

Prisma sandboxes are rate limited:

- 10 requests per 10 seconds (on average)
- If this rate is exceeded, requests are being queued in memory. If this queue exceeds 25 requests, an error is returned immediately.
- The header field `throttled-by` is included in HTTP responses. It indicates how long the request was delayed due to throttling (in milli seconds).

The upper bound in storage capacity for a Prisma API that's running in a Sandbox is 100 MB.

</InfoBox>

When you're running `prisma1 deploy` for a Prisma API, there are two scenarios with respect to the targeted Prisma server:

- The `endpoint` property in `prisma.yml` is **specified**. In this case, the CLI will directly deploy the API to that endpoint.
- The `endpoint` property in `prisma.yml` is **not specified**. In this case, the Prisma CLI wizard will prompt you with a few questions, construct the `endpoint` for you and deploy the API to the corresponding server. It also writes the `endpoint` into `prisma.yml`, this will be used as the default for future deploys. To bring up the wizard again, you can run `prisma1 deploy --new` or remove the `endpoint` manually from `prisma.yml`.

## Channels

When installing Prisma you can pick between 3 different channels:

- **stable** is a production-ready release that has been thoroughly tested.
- **beta** is a release that is currently undergoing extensive testing before being promoted to stable.
- **alpha** is updated every time a new feature or bugfix lands in Prisma.

If you follow a quickstart or any other material in the documentation, you will be installing from the stable channel. To install Prisma from the beta or alpha channel, follow these steps:

### Installing Prisma Beta

Using npm:

```sh
npm install -g prisma1@beta
```

If you are using the docker images directly, you can find the latest beta image on https://hub.docker.com/r/prismagraphql/prisma/tags/ or simply use the `beta` tag to always get the latest.

### Installing Prisma Alpha

Using npm:

```sh
npm install -g prisma1@alpha
```

If you are using the docker images directly, you can find the latest alpha image on https://hub.docker.com/r/prismagraphql/prisma/tags/ or simply use the `alpha` tag to always get the latest.

## Authentication

### Docker

Prisma servers support _symmetric_ as well as _asymmetric_ authentication approaches.

#### Asymmetric authentication using public/private key pairs

Using the asymmetric authentication approach, Prisma servers are secured using public/private key pairs. 

The Prisma servers knows the **public key** (via the `legacySecret` property in `docker-compose.yml`, see [here](!alias-aira9zama5) for more info). The **private key** is known locally by the Prisma CLI and used to generate authentication tokens. These tokens are used to authenticate requests against the Prisma server (e.g. an invocation of `prisma1 deploy`) which can then be validated by the Prisma server using its public key.

#### Symmetric approach using a single secret

In Prisma 1.7, a new authentication approach for Prisma servers has been introduced. It uses a single secret to authenticate requests made by the Prisma CLI against a Prisma server.

The secret can be chosen by the admin of the Prisma server. It is set via the `managementApiSecret` property in the `docker-compose.yml` which is used to deploy the Prisma server. If not specified, the CLI doesn't need to authenticate its requests. If specified, the CLI needs to have access to an environment variable called `PRISMA_MANAGEMENT_API_SECRET` which contains the secret, otherwise the CLI can not talk to the Prisma server (e.g. `prisma1 deploy` will fail).

### Prisma Cloud

#### Login

The CLI authenticates against Prisma Cloud using the `cloudSessionKey` stored in `~/.prisma/config.yml`. The CLI writes that key into that file upon the initial login: `prisma1 login`.

#### Logout

To logout from the Prisma CLI remove the `cloudSessionKey` from `~/.prisma/config.yml` file.
