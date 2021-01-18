---
alias: cheeni9mae
description: Display service information (endpoints, cluster, ...)
---

# `prisma info`

Display service information:

- All [clusters](!alias-eu2ood0she) to which the service is currently deployed
- Stage
- API endpoints (HTTP and Websocket)

#### Usage

```sh
prisma info
```

#### Flags

```
 -e, --env-file ENV-FILE    Path to .env file to inject env vars
 -j, --json                 JSON Output
 -s, --secret               Print secret in JSON output (requires --json option)
```

#### Examples

##### Print information about current service.

```sh
prisma info
```

##### Print information about current service in JSON.

```sh
prisma info --json
```

##### Print information about current service in JSON and include service secret.

```sh
prisma info --json --secret
```

> **Note**: The secret will not be printed if the `--json` flag is not provided.