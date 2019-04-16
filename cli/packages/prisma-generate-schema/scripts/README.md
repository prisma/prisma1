## Scripts to re-generate unit tests

These scripts re-generate the ground-truth schema for all unit tests from an existing prisma service.

### Workflow

Workfow for re-generating tests using these scripts. Please make sure you are in the correct working directory when executing them.

#### Automated

0. Start in this directory
1. `./refreshAll.sh`

#### Manual

0. Start in this directory.
1. `cd mongodb` or `cd mysql`.
1. (optional) docker-compose up (if you want to run this locally).
1. Set the `PRISMA_HOST` environment variable to your prisma host.
1. Execute `../deploySchemas.sh`, which will deploy all test models to `https://$PRISMA_HOST:4466/schema-generator/MODEL`.
1. `cd ..`
1. `./fetchSchemas NAME`, where name is usually `relational` or `document`, depending on which DB is running on your `PRISMA_HOST`.
1. If you used docker, you can stop your containers again.
