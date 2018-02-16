## Local Development

```sh
$ git clone git@github.com:graphcool/prisma.git
$ cd prisma/cli
$ yarn install
$ cd packages/prisma-cli-engine
$ yarn build
$ cd ../prisma-cli-core
$ yarn build
$ cd ../prisma-cli
$ yarn build
$ node dist/index.js
```
