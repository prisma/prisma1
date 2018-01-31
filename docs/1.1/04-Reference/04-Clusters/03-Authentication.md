---
alias: poh9au8ahy
description: Authentication
---

# Authentication

Clusters are secured using public/private key pairs. The cluster knows the public key. The private key is known locally by the Prisma CLI and used to generate _cluster tokens_. These cluster tokens are used to authenticate requests against the cluster (e.g. an invokation of `prisma deploy`) which can then be validated by the cluster using the public key.