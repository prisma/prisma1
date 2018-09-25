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

## Dependencies

To understand the current dependencies setup of Prisma, please have a look at [ARCHITECTURE.md](https://github.com/prisma/prisma/blob/master/cli/ARCHITECTURE.md)

## Cache

You might need to set the environment variable `GRAPHCOOL_CLI_CLEAR_CACHE` to some value to prevent overeager caching of your code.

## Testing

To recreate jest snapshots:

```
cd packages/prisma-cli-core
yarn test -- -u
```
