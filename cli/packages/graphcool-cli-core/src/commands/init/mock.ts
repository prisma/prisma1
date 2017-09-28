import { ProjectDefinition } from 'graphcool-cli-engine'

export const definitionWithModule: ProjectDefinition = {
  "modules": [
  {
    "name": "",
    "content": "types: ./types.graphql\nmodules:\n  github: modules/github/graphcool.yml\npermissions:\n  - operation: '*'\nrootTokens: []\n",
    "files": {
      "./types.graphql": "# This file contains the GraphQL Types\n# Graphcool has one special type, the File type:\n\n# type File implements Node {\n#   contentType: String!\n#   createdAt: DateTime!\n#   id: ID! @isUnique\n#   name: String!\n#   secret: String! @isUnique\n#   size: Int!\n#   updatedAt: DateTime!\n#   url: String! @isUnique\n# }\n\n# All types need to have an id field defined like this:\n\n# type User implements Node {\n#   id: ID! @isUnique\n# }\n\n"
    },
    "baseDir": "/Users/tim/code/cli-tests/9000",
    "definition": {
      "types": "./types.graphql",
      "modules": {
        "github": "modules/github/graphcool.yml"
      },
      "permissions": [
        {
          "operation": "*"
        }
      ],
      "rootTokens": []
    }
  },
  {
    "name": "github",
    "content": "# GraphQL types\ntypes: ./types.graphql\n\n# functions\nfunctions:\n  github-authentication:\n    handler:\n      code:\n        src: ./code/github-authentication.js\n        environment:\n          CLIENT_ID: ${env:CLIENT_ID}\n          CLIENT_SECRET: ${env:CLIENT_SECRET}\n    type: resolver\n    schema: ./schemas/github-authentication.graphql\n\n# Permanent Auth Token / Root Tokens\nrootTokens:\n- github-authentication\n",
    "files": {
      "./types.graphql": "type GithubUser {\n  id: ID! @isUnique\n  createdAt: DateTime!\n  githubUserId: String @isUnique\n  updatedAt: DateTime!\n}\n",
      "./code/github-authentication.js": "const fromEvent = require('graphcool-lib').fromEvent\n\n// read Github credentials from environment variables\nconst client_id = process.env.CLIENT_ID\nconst client_secret = process.env.CLIENT_SECRET\n\nmodule.exports = function (event) {\n  console.log(event)\n\n  if (!process.env.CLIENT_ID || !process.env.CLIENT_SECRET) {\n    console.log('Please provide a valid client id and secret!')\n    return { error: 'Github Authentication not configured correctly.'}\n  }\n\n  if (!event.context.graphcool.pat) {\n    console.log('Please provide a valid root token!')\n    return { error: 'Github Authentication not configured correctly.'}\n  }\n\n  const code = event.data.githubCode\n  const graphcool = fromEvent(event)\n  const api = graphcool.api('simple/v1')\n\n  function getGithubToken () {\n    console.log('Getting access token...')\n\n    return fetch('https://github.com/login/oauth/access_token', {\n      method: 'POST',\n      headers: {\n        'Content-Type': 'application/json',\n        'Accept': 'application/json'\n      },\n      body: JSON.stringify({\n        client_id,\n        client_secret,\n        code\n      })\n    })\n    .then(data => data.json())\n    .then(json => {\n      if (json.error) {\n        throw new Error(json)\n      } else {\n        console.log(json)\n\n        return json.access_token\n      }\n    })\n    .catch(error => {\n      console.log(error)\n\n      return Promise.reject({ message: 'Error while authenticating with Github' })\n    })\n  }\n\n  function getGithubAccountData (githubToken) {\n    console.log('Getting account data...')\n\n    return fetch(`https://api.github.com/user?access_token=${githubToken}`)\n      .then(response => response.json())\n      .then(json => {\n        if (json.error) {\n          throw new Error(json)\n        } else {\n          console.log(json)\n\n          return json\n        }\n      })\n      .catch(error => {\n        console.log(error)\n\n        return Promise.reject({ message: 'Error while getting Github user data' })\n      })\n  }\n\n  function getGraphcoolUser (githubUser) {\n    console.log('Getting Graphcool user...')\n\n    return api.request(`\n      query {\n        GithubUser(githubUserId: \"${githubUser.id}\") {\n          id\n        }\n      }\n    `)\n    .then(response => {\n      if (response.error) {\n        throw new Error(response)\n      } else {\n        console.log(response)\n\n        return response.GithubUser\n      }\n    })\n    .catch(error => {\n      console.log(error)\n\n      return Promise.reject({ message: 'Error while getting Graphcool user' })\n    })\n  }\n\n  function createGraphcoolUser (githubUser) {\n    console.log('Creating Graphcool user...')\n\n    return api.request(`\n      mutation {\n        createGithubUser(\n          githubUserId:\"${githubUser.id}\"\n        ) {\n          id\n        }\n      }\n    `)\n    .then(response => {\n      if (response.error) {\n        throw new Error(response)\n      } else {\n        console.log(response)\n\n        return response.createGithubUser.id\n      }\n    })\n    .catch(error => {\n      console.log(error)\n\n      return Promise.reject({ message: 'Error while creating Graphcool user' })\n    })\n  }\n\n  function generateGraphcoolToken (graphcoolUserId) {\n    return graphcool.generateAuthToken(graphcoolUserId, 'GithubUser')\n      .catch(error => {\n        console.log(error)\n\n        return Promise.reject({ message: 'Error while generating token' })\n      })\n  }\n\n  return getGithubToken()\n    .then(githubToken => {\n      return getGithubAccountData(githubToken)\n        .then(githubUser => {\n          return getGraphcoolUser(githubUser).then(graphcoolUser => {\n            if (graphcoolUser === null) {\n              return createGraphcoolUser(githubUser)\n            } else {\n              return graphcoolUser.id\n            }\n          })\n        })\n        .then(generateGraphcoolToken)\n        .then(token => {\n          return { data: { token } }\n        })\n    })\n    .catch((error) => {\n      console.log(error)\n\n      return { error: error.message }\n    })\n}\n",
      "./schemas/github-authentication.graphql": "type AuthenticateGithubUserPayload {\n  token: String!\n}\n\nextend type Mutation {\n  authenticateGithubUser(githubCode: String!): AuthenticateGithubUserPayload\n}\n"
    },
    "baseDir": "/Users/tim/code/cli-tests/9000/modules/github",
    "definition": {
      "types": "./types.graphql",
      "functions": {
        "github-authentication": {
          "handler": {
            "code": {
              "src": "./code/github-authentication.js",
              "environment": {}
            }
          },
          "type": "resolver",
          "schema": "./schemas/github-authentication.graphql"
        }
      },
      "rootTokens": [
        "github-authentication"
      ]
    }
  }
]
}
