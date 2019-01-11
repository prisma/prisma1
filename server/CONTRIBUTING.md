### How to write your first test

This tutorial aims to get you started with testing your API queries against ApiTestServer, a utility provided to do initial API testing. For this, little knowledge of [Scala SBT](https://www.scala-sbt.org/) and [ScalaTest](http://www.scalatest.org/) would be helpful.

**Setting up Prisma**
1. Make sure you have `sbt` installed.
1. Clone the repo: `git clone https://github.com/prisma/prisma.git`
1. Run `cd prisma/server`
1. Make the environment variables from the file [.envrc](https://github.com/prisma/prisma/blob/master/server/.envrc) available. You can either do this manually by running `source .envrc` in your shell. This can also be automated with the program `direnv`, which will automatically set the environment variables from this file when you are inside the `server` folder. On a Mac with Homebrew you can get it via `brew install direnv`.
1. Run `sbt update` to fetch all the required packages.
1. Now you need to choose which connector your test should use. A test always runs against the currently chosen connector. Setting the connector is possible by running a make command, e.g. `make dev-mysql`. This command will start a docker container for the selected database and configure the tests to use it. This [Makefile](https://github.com/prisma/prisma/blob/master/server/Makefile) contains all possible connector configurations. 


**Writing your First Test**

1. API test files are written at location `{Prisma_Directory}/server/servers/api/src/test/scala/com/prisma/api`. Go ahead and create your test file class and follow the naming conventions already used there. For example `JsonVariablesSpec`.
1. The test class that you have created should extend traits `FlatSpec` and `Matchers` which are part of ScalaTest and `ApiSpecBase` which is trait provided in Prisma.
1. The next thing is to define a schema against which you will test your APIs. For that, we will be using `com.prisma.shared.schema_dsl.SchemaDsl`. Import this in your test file and declare your schema. There different ways to declare schema. An example would be
    ```
         val project = SchemaDsl.fromString() {
            """
              | type MyRequiredJson {
              |   id: ID! @unique
              |   json: Json!
              | }
            """.stripMargin
          }
    ```
    PS: If your are interested in declaring complex schema then there are a lot of test cases in package `com.prisma.api.queries`. You can check them out. They help you in setting up a complex schema.

1. After that, we will set up the database with the schema that we just created. For that call
    ```
        database.setup(project)
    ```
    `database` is an instance of `ApiTestDatabase` which comes with trait `ApiSpecBase` and by the usage, you can guess it that it is helping as a database utility.

1. Now once the schema is set up at the backend, it is time to test a query. An example would be like :
    ```scala
        val queryString = """mutation createMyRequiredJson($json: Json!) {
        | createMyRequiredJson(data: {json: $json}) {
        |  id
        |  json
        | }
        |}"""
    ```
    For a detailed explanation on making a query, please refer Prisma Documentation.
1. Testing this query is really simple. In `ApiSpecBase` we already have a `server` variable which is a type of `ApiTestServer`. This variable simulates a http end point of Prisma Server. An example usage would be like:
    ```scala
        val variables = Json.parse("""{"json": {"test": 1}}""")

        server.query(queryString, project, variables = variables)
    ```

    For a detailed explanation on making a query, please refer Prisma Documentation.
1. Running the code is also super easy. Just go back to your sbt console that we had setup in this tutorial and running the following command.
    ```
        testOnly <com.prisma.api.your.test.file>
    ```
    This will only test your test cases. If you want to run all the tests, run the following command.
    ```
        test
    ```

1. Congratulations! You ran your first test case.
