# Travis

This example demonstrates how a CI setup using [Travis CI](https://travis-ci.com/) can look like.

## Get started

> **Note**: The project setup in this example is minimalistic. You can use it as a reference example for your own project, but you might want to evolve it into a more advanced setup. For example, you can use [encrypted environment variables](https://docs.travis-ci.com/user/environment-variables/#Defining-encrypted-variables-in-.travis.yml) for sensitive information like the Prisma API secret.

### 1. Download the example

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/prismagraphql/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/travis
```

### 2. Set up repository locally and on GitHub

Next, navigate into the downloaded folder and initiate a new git repository:

```sh
cd travis
git init .
```

In your GitHub account, create a new repository and copy its git URL, for example:

`git@github.com:marktani/travis-prisma.git`

Add a new remote to your local git repository:

```sh
git remote add origin git@github.com:marktani/travis-prisma.git
```

### 3. Link GitHub and Travis CI

Now, link your GitHub account in your [Travis CI](https://travis-ci.com/) account, and push the downloaded example to your repository:

```sh
git push --set-upstream origin master
```

### 4. Verify the build

In your Travis CI account, review the build and confirm that the build went through.

Here is a description of the performed steps that are expected to happen (compare to `.travis.yml` as well):

1.  We install the `npm` dependencies with `npm install`.
2.  We spin up a new Prisma server and database using `docker-compose up -d`.
3.  We confirm that the Docker setup is correct with `docker ps`.
4.  We sleep 20 seconds to allow for the two Docker containers to finish their setup.
5.  We run `prisma deploy` with the locally installed version of Prisma. This will set up a new service `travis` at stage `test` and seed some data according to `prisma.yml`.
6.  We run a short test script that queries the new service and checks the result against the expected result in `test.txt`.
7.  We output a success message.
