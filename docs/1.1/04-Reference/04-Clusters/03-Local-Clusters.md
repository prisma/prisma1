---
alias: si4aef8hee
description: Overview
---

# Local Cluster

This chapter describes advanced topics for the local Prisma cluster. To learn more about local cluster deployment, read [this tutorial](!alias-meemaesh3k).

## Debugging

You can view logs from your local Prisma cluster to debug issues.

### Logs

You can view normal debug logs:

```sh
prisma local logs
```

### Docker logs

If you need more extensive logs you can view the raw logs from the containers running MySQL and Prisma:

```sh
docker logs prisma

docker logs prisma-db
```

### Verify Docker containers

If you get an error message saying `Error response from daemon: No such container` you can verify that the containers are running:

```sh
docker ps
```

You should see output similar to this:

```
❯ docker ps
CONTAINER ID  IMAGE                       COMMAND                 CREATED            STATUS            PORTS                   NAMES
7210106b6650  prismagraphql/prisma:1.0.0  "/app/bin/single-ser…"  About an hour ago  Up About an hour  0.0.0.0:4466->4466/tcp  prisma
1c15922e15ba  mysql:5.7                   "docker-entrypoint.s…"  About an hour ago  Up About an hour  0.0.0.0:3306->3306/tcp  prisma-db
```

### Nuke

If your local prisma cluster is in an unrecoverable state, the easiest option might be to completely reset it. Be careful as **this command will reset all data** in your local cluster.

```sh
❯ prisma local nuke
Nuking local cluster 10.9s
Booting fresh local development cluster 18.4s
```
