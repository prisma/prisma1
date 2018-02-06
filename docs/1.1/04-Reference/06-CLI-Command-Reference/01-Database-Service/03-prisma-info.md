---
alias: cheeni9mae
description: Display service information (endpoints, cluster, ...)
---

# `prisma info`

Display service information:

- All [clusters](!alias-eu2ood0she) to which the service is currently deployed
- API endpoints

#### Usage

```sh
prisma info
```

#### Flags

```
 -c, --current              Only show info for current service
 -e, --env-file ENV-FILE    Path to .env file to inject env vars
 -j, --json                 Json Output
 -s, --secret               Print secret in json output
```

#### Examples

##### Print information about current service.

```sh
prisma info --current
```