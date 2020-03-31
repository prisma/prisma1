/* tslint:disable */
import { ProjectDefinition } from 'prisma-cli-engine'

export const defaultPjson = {
  name: '',
  version: '1.0.0',
  description: 'My Prisma Service',
}

export const defaultDefinition: ProjectDefinition = {
  modules: [
    {
      name: '',
      content: `\
# Welcome to Prisma!
#
# This file is the main config file for your Prisma Service.
# It's very minimal at this point and uses default values.
# We've included a hello world function here.
# Just run \`prisma1 deploy\` to have the first running Prisma Service.
#
# Check out some examples:
#    https://github.com/prisma/framework/tree/master/examples
#
# Here are the reference docs of this definition format:
# https://www.graph.cool/docs/reference/service-definition/prisma.yml-foatho8aip
#
# Happy Coding!


# In the types.graphql you define your data schema
types: ./types.graphql


functions:
  hello:
    handler:
      code: src/hello.js
    type: resolver
    schema: src/hello.graphql


# Model/Relation permissions are used to limit the API access
# To take the burden of thinking about those while development, we
# preconfigured the wildcard ("*") permission that allows everything
# Read more here:
# https://www.graph.cool/docs/reference/auth/authorization/overview-iegoo0heez
permissions:
  - operation: "*"


# Your root tokens used for functions to get full access to the API
# Read more here:
# https://www.graph.cool/docs/reference/auth/authentication/authentication-tokens-eip7ahqu5o
# rootTokens:
#   - mytoken

`,
      files: {
        './types.graphql': `\
# The following types define the data model of the example service
# based on which the GraphQL API is generated

type User @model {
  id: ID! @isUnique
  name: String
  dateOfBirth: DateTime

  # Uncomment below - you can declare relations between models like this

  # posts: [Post!]! @relation(name: "UserPosts")
}


# Uncomment the model below as well

# type Post @model {
#   id: ID! @isUnique
#   title: String!
#
#   # Every relation also required a back-relation (to determine 1:1, 1:n or n:m)
#   author: User! @relation(name: "UserPosts")
# }
`,
        './src/hello.js': `\
export default async event => {
  // you can use ES7 with async/await and even TypeScript in your functions :)

  await new Promise(r => setTimeout(r, 50))

  return {
    data: {
      message: \`Hello $\{event.data.name || 'World'\}\`
    }
  }
}`,
        './src/hello.graphql': `\
type HelloPayload {
  message: String!
}

extend type Query {
  hello(name: String): HelloPayload
}
`,
      },
    },
  ],
}

export const changedDefaultDefinition: ProjectDefinition = {
  modules: [
    {
      name: '',
      content: `\
# This is the changed default definition, used in tests
#
# This file is the main config file for your Prisma Service.
# It's very minimal at this point and uses default values.
# We've included a hello world function here.
# Just uncomment it and run \`prisma1 deploy\`
#
# Check out some examples:
#    https://github.com/prisma/framework/tree/master/examples
#
# Happy Coding!


# GraphQL types
types: ./types.graphql


# uncomment this:

# functions:
#   hello:
#     handler:
#       code:
#         src: ./code/hello.js
#     type: resolver
#     schema: ./code/hello.graphql


# Prisma modules
modules: {}


# Model/Relation permissions
permissions:
- operation: "*"


# Permanent Auth Token / Root Tokens
rootTokens: []

`,
      files: {
        './types.graphql': `\
# This file contains the GraphQL Types

# All types need to have the three fields id, updatedAt and createdAt like this:

type User implements Node {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
}


# Prisma has one special type, the File type:

# type File implements Node {
#   contentType: String!
#   createdAt: DateTime!
#   id: ID! @isUnique
#   name: String!
#   secret: String! @isUnique
#   size: Int!
#   updatedAt: DateTime!
#   url: String! @isUnique
# }

type Post implements Node {
  id: ID! @isUnique
  title: String
}
`,
        './code/hello.js': `\
module.exports = event => {
  return {
    data: {
      message: \`Hello $\{event.data.name || 'World'\}\`
    }
  }
}`,
        './code/hello.graphql': `\
type HelloPayload {
  message: String!
}

extend type Query {
  hello(name: String): HelloPayload
}
`,
      },
    },
  ],
}

export const advancedDefinition: ProjectDefinition = {
  modules: [
    {
      name: '',
      content:
        'types: ./types.graphql\nfunctions:\n  filter-posts:\n    handler:\n      code:\n        src: ./code/filter-posts.js\n    type: operationBefore\n    operation: Post.create\n  log-posts:\n    handler:\n      code:\n        src: ./code/log-posts.js\n    type: subscription\n    query: ./code/log-posts.graphql\n  weather:\n    handler:\n      code:\n        src: ./code/weather.js\n    type: resolver\n    schema: ./code/weather.graphql\npermissions:\n- operation: File.read\n- operation: File.create\n- operation: File.update\n- operation: File.delete\n- operation: Post.read\n- operation: Post.update\n- operation: Post.delete\n- operation: Post.create\n  authenticated: true\n- operation: User.read\n- operation: User.create\n- operation: User.update\n- operation: User.delete\nrootTokens: []\n',
      files: {
        './types.graphql':
          'type File implements Node {\n  contentType: String!\n  createdAt: DateTime!\n  id: ID! @isUnique\n  name: String!\n  secret: String! @isUnique\n  size: Int!\n  updatedAt: DateTime!\n  url: String! @isUnique\n}\n\ntype User implements Node {\n  createdAt: DateTime!\n  id: ID! @isUnique\n  updatedAt: DateTime!\n}\n\ntype Post implements Node {\n  title: String!\n  description: String!\n  createdAt: DateTime!\n  id: ID! @isUnique\n  updatedAt: DateTime!\n}',
        './code/log-posts.js':
          '// Click "EXAMPLE EVENT" to see whats in `event`\nmodule.exports = function (event) {\n  console.log(event.data)\n  return {data: event.data}\n}\n',
        './code/log-posts.graphql':
          'subscription {\n  Post(filter: {\n    mutation_in: [CREATED, UPDATED, DELETED]\n  }) {\n    updatedFields\n    node {\n      id\n    }\n  }\n}',
        './code/filter-posts.js':
          "// Click \"EXAMPLE EVENT\" to see whats in `event`\nmodule.exports = function (event) {\n  console.log(event.data)\n  if (event.data.createPost.description.includes('bad') {\n  \treturn {error: 'bad is not allowed'}\n  }\n  return {data: event.data}\n}\n",
        './code/weather.js':
          "const fetch = require('node-fetch')\n\nmodule.exports = function (event) {\n  const city = event.data.city\n  return fetch(getApiUrl(city))\n  .then(res => res.json())\n  .then(data => {\n    console.log(data)\n    return {\n      data: {\n        temperature: data.main.temp,\n        pressure: data.main.pressure,\n        windSpeed: data.wind.speed,\n      }\n    }\n  })\n}\n\nfunction getApiUrl(query) {\n\treturn `http://samples.openweathermap.org/data/2.5/weather?q=${query}&appid=b1b15e88fa797225412429c1c50c122a1`\n  }",
        './code/weather.graphql':
          'type WeatherPayload {\n  temperature: Float!\n  pressure: Float!\n  windSpeed: Float!\n}\n\nextend type Query {\n  weather(city: String!): WeatherPayload\n}\n',
      },
    },
  ],
}

export const examples = {
  blank: defaultDefinition,
  instagram: advancedDefinition,
  stripe: advancedDefinition,
  sendgrid: advancedDefinition,
}
