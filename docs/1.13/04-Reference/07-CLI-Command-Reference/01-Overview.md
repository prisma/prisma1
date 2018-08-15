---
alias: je3ahghip5
description: Learn how to use the Prisma CLI
---

# Overview

The Prisma command line interface (CLI) is the primary tool to manage your database services with Prisma.

Generally, the configuration of a Prisma service is handled using the CLI and the service definition file [`prisma.yml`](!alias-foatho8aip).

A central part of configuring a Prisma service is deploying a [data model](!alias-eiroozae8u).

## Getting Started

You can download the Prisma CLI from npm:

```sh
npm install -g prisma
```

To initialize a new service, use the `init` command. Then follow the interactive prompt to bootstrap the service based on a template of your choice:

```sh
prisma init hello-world
```

In the following sections you'll learn more about configuring Prisma services using the CLI.

## Using a HTTP proxy for the CLI

The Prisma CLI supports [custom HTTP proxies](https://github.com/graphcool/prisma/issues/618). This is particularly relevant when being behind a corporate firewall.

To activate the proxy, provide the env vars `HTTP_PROXY` and `HTTPS_PROXY`. The behavior is very similar to how the [`npm` CLI handles this](https://docs.npmjs.com/misc/config#https-proxy).

The following environment variables can be provided:

- `HTTP_PROXY` or `http_proxy`: Proxy url for http traffic, for example `http://localhost:8080`
- `HTTPS_PROXY` or `https_proxy`: Proxy url for https traffic, for example `https://localhost:8080`
- `NO_PROXY` or `no_proxy`: To disable the proxying for certain urls, please provide a glob for `NO_PROXY`, for example `*`.

To get a simple local proxy, you can use the [`proxy` module](https://www.npmjs.com/package/proxy):

```bash
npm install -g proxy
DEBUG="*" proxy -p 8080
HTTP_PROXY=http://localhost:8080 HTTPS_PROXY=https://localhost:8080 prisma deploy
```
