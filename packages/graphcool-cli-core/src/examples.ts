/* tslint:disable */
import { ProjectDefinition } from 'graphcool-cli-engine'

export const defaultDefinition: ProjectDefinition = {
  modules: [
    {
      name: '',
      content: `\
# Welcome to Graphcool!
#
# This file is the main config file for your Graphcool Project.
# It's very minimal at this point and uses default values.
# We've included a hello world function here.
# Just uncomment it an run \`graphcool deploy\`
#
# For full config options, check the docs:
#    graph.cool/docs
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
#     schema: ./schemas/hello.graphql

 
# Graphcool modules
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
# Graphcool has one special type, the File type:

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

# All types need to have an id field defined like this:

# type User implements Node {
#   id: ID! @isUnique
# }

`,
        './code/hello.js': `\
module.exports = event => {
  return {
    data: {
      message: \`Hello $\{event.data.name || 'World'\}\`
    }
  }
}`,
        './schemas/hello.graphql': `\
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
      content: `
types: ./types.graphql
functions: {}
permissions:
- isEnabled: true
  operation: File.read
  authenticated: false
- isEnabled: true
  operation: File.create
  authenticated: false
- isEnabled: true
  operation: File.update
  authenticated: false
- isEnabled: true
  operation: File.delete
  authenticated: false
- isEnabled: true
  operation: User.read
  authenticated: false
- isEnabled: true
  operation: User.create
  authenticated: false
- isEnabled: true
  operation: User.update
  authenticated: false
- isEnabled: true
  operation: User.delete
  authenticated: false
rootTokens: []
`,
      files: {
        './types.graphql': `type File implements Node {
  contentType: String!
  createdAt: DateTime!
  id: ID! @isUnique
  name: String!
  secret: String! @isUnique
  size: Int!
  updatedAt: DateTime!
  url: String! @isUnique
}

type User implements Node {
  createdAt: DateTime!
  id: ID! @isUnique
  updatedAt: DateTime!
}

type Post {
  imageUrl: String!
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
