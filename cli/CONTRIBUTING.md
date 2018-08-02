## Local Development

```sh
$ git clone git@github.com:graphcool/prisma.git
$ cd prisma/cli
$ yarn install
$ cd packages/prisma-yml
$ yarn install && yarn build
$ cd ../prisma-cli-engine
$ yarn install && yarn build
$ cd ../prisma-db-introspection
$ yarn install && yarn build
$ cd ../prisma-cli-core
$ yarn install && yarn build
$ cd ../prisma-cli
$ yarn install && yarn build
$ node dist/index.js
```

To execute the tests you have to [set up a local prisma server](<https://www.prisma.io/docs/tutorials/deploy-prisma-servers/local-(docker)-meemaesh3k>) with a postgres database and expose the port `5432` to localhost.
Once you deployed your local prisma server, you have to set up the following environment variables:

- TEST_PG_USER
- TEST_PG_PASSWORD
- TEST_PG_DB

To recreate jest snapshots:

```
cd packages/prisma-cli-core
yarn test -- -u
```
