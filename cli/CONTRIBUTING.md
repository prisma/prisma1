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

To recreate jest snapshots:

```
cd packages/prisma-cli-core
yarn test -- -u
```
