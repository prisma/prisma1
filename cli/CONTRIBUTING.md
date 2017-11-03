## Local Development

```sh
$ git clone git@github.com:graphcool/framework.git
$ cd framework/cli
$ yarn install
$ cd packages/graphcool-cli-engine
$ yarn build
$ cd ../graphcool-cli-core
$ yarn build
$ cd ../graphcool-cli
$ yarn build
$ node dist/index.js
```
