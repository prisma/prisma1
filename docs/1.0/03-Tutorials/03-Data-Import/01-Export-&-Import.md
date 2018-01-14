---
alias: caith9teiy
description: Learn how to export and import data with Graphcool.
---

# Export & Import

Graphcool uses a dedicated, intermediate format for importing and exporting data: The [Normalized Data Format](!alias-teroo5uxih) (NDF).

```
+--------------+                    +----------------+                       +------------+
| +--------------+                  |                |                       |            |
| |            | |                  |                |                       |            |
| | SQL        | |  (1) transform   |      NDF       |  (2) chunked upload   |  graphcool |
| | MongoDB    | | +--------------> |                | +-------------------> |            |
| | JSON       | |                  |                |                       |            |
| |            | |                  |                |                       |            |
+--------------+ |                  +----------------+                       +------------+
  +--------------+
```

In this tutorial, you'll perform the following steps:

1. Create a Graphcool service
1. Seed some initial data for the service
1. Export the data in NDF
1. Deploy the service to a new stage
1. Import the data in NDF

## Create a Graphcool service

<Instruction>

In your terminal, navigate to a folder of your choice and run the following command:

```sh
graphcool init import-example
```

</Instruction>

<Instruction>

When prompted what kind of template to use, choose the `Minimal setup: database-only` one.

</Instruction>

This created a new directory called `import-example` with the root configuration file `graphcool.yml` as well as the definition of the service's data model in `datamodel.graphql`.

Next, you'll update the data model to also include a relation.

<Instruction>

Open `datamodel.graphql` and change the contents to looks as follows:

```graphql
type User {
  id: ID! @unique
  name: String!
  posts: [Post!]!
}

type Post {
  id: ID! @unique
  title: String!
  author: User!
}
```

## Seed initial data

Next, you're going to seed some initial data for the service.

<Instruction>

Create a new file called `seed.graphql` inside the `import-example` directory and add the following mutation to it:

```sh
mutation {
  createUser(data: {
    name: "Sarah",
    posts: {
      create: [
        { title: "GraphQL is awesome" },
        { title: "It really is" },
        { title: "How to GraphQL is the best GraphQL tutorial" }
      ]
    }
  }) {
    id
  }
}
```

</Instruction>

Now you need to tell the CLI that you created this file. You can do so by setting the `seed` property in `graphcool.yml`.

<Instruction>

Open `graphcool.yml` and update its contents to look as follows:

```yml
service: import-example
stage: dev

datamodel: datamodel.graphql

# to enable auth, provide
# secret: my-secret
disableAuth: true

seed:
  import: seed.graphql
```

</Instruction>

When deploying the service, the CLI will now send the mutation defined in `seed.graphql` to your service's API.

<Instruction>

Deploy the Graphcool service by running the following command:

```sh
graphcool deploy
```

</Instruction>

<Instruction>

When prompted where (i.e. to which _cluster_) to deploy your Graphcool service, choose one of the _public cluster_ options: `graphcool-eu1` or `graphcool-us1`. (Note that seeding also works when deploying with Docker)

</Instruction>

The CLI now deploys the service and executes the mutation in `seed.graphql`. To convince yourself the seeding actually worked, you can open up a GraphQL Playground and send the following query:

```graphql
{
  users {
    name
    posts {
      title
    }
  }
}
```

The Graphcool API will respond with the following data:

```json


{
  "data": {
    "users": [
      {
        "name": "Sarah",
        "posts": [
          {
            "title": "GraphQL is awesome"
          },
          {
            "title": "It really is"
          },
          {
            "title": "How to GraphQL is the best GraphQL tutorial"
          }
        ]
      }
    ]
  }
}
```

## Export the data in NDF

It's time to export the in the Normalized Data Format.

<Instruction>

In the `import-example` in your terminal, execute the following command:

```sh
graphcool export
```

</Instruction>

This creates a new file called `export-__TIMESTAMP__.zip` where `__TIMESTAMP__` represents the exact time of the export. The files in the zip directory are in NDF. To learn more about the structure, check out the [reference documentation for the NDF](!alias-teroo5uxih).

## Deploy the service to a new stage

Next, you'll create "clone" of the service by deploying it to a new stage.

<Instruction>

Open `graphcool.yml` and set the `stage` property to a new value. Also remove the `seed` and `cluster` properties!

```sh
service: import-example
stage: test

datamodel: datamodel.graphql

# to enable auth, provide
# secret: my-secret
disableAuth: true
```

</Instruction>

<Instruction>

Run `graphcool deploy` again to deploy the service the new `test` stage.

</Instruction>

<Instruction>

Like before, when prompted where to deploy your Graphcool service, either choose `graphcool-eu1` or `graphcool-us1`.

</Instruction>

## Import the data in NDF

Now that the service is running, you can import the data from the zip directory!

<Instruction>

Run the following command in your terminal. Note that you need to replace the `__DATA__` placeholder with the path to the exported zip directory (e.g. `export-2018-01-13T19:28:25.921Z.zip`):

```sh
graphcool import --data __DATA__
```

</Instruction>

That's it! To convince yourself the import actually worked, you can open up a GraphQL Playground for the current `test` stage and send the above query again.