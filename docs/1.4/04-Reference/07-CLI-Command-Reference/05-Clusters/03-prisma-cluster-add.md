---
alias: quungoogh8
description: Add an existing cluster.
---

# `prisma cluster add`

Add a new cluster to the [cluster registry](!alias-eu2ood0she#cluster-regsitry).

#### Usage

```sh
prisma cluster add
```

#### Flags

```
 -n, --name NAME            Cluster name
 -e, --endpoint ENDPOINT    Cluster endpoint
 -s, --secret SECRET        Cluster secret (optional)
```

#### Examples

##### For interactive usage execute command without flags.

```sh
prisma cluster add
```

##### For non-interactive usage execute command with flags --name, --endpoint and optional --secret.

```sh
prisma cluster add --name <cluster name> --endpoint <cluster host> --secret <cluster secret>
```