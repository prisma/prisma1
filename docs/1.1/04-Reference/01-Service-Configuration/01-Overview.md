---
alias: ieshoo5ohm
description: Learn how to use the Prisma command line
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
prisma init
```

In the following sections you'll learn more about configuring Prisma services using the CLI.
