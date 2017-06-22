import {modifiedTwitterSchema, simpleTwitterSchemaWithSystemFields, modifiedTwitterSchemaJSONFriendly} from './schemas'

/*
 * Testing
 */
export const testToken = 'abcdefghijklmnopqrstuvwxyz'

export const mockConfigFile = `\
{
  "token": "1234"
}\
`

export const mockProjectFile1 = `\
# project: abcdefghijklmn
# version: 1

${simpleTwitterSchemaWithSystemFields}
`

export const clonedMockProjectFile1 = `\
# project: nmlkjihgfedcba
# version: 1

${simpleTwitterSchemaWithSystemFields}
`

export const mockProjectFileWithAlias1 = `\
# project: example
# version: 1

${simpleTwitterSchemaWithSystemFields}
`

export const mockModifiedProjectFile1 = `\
# project: abcdefghijklmn
# version: 1

${modifiedTwitterSchema}
`

export const mockProjectFile2 = `\
# project: abcdefghijklmn
# version: 2

${modifiedTwitterSchema}
`

export const mockProjectFileWithAlias2 = `\
# project: example
# version: 2

${simpleTwitterSchemaWithSystemFields}
`

export const mockProjectFileWithUppercaseAlias1 = `\
# project: Example
# version: 1

${simpleTwitterSchemaWithSystemFields}
`

export const mockProjectFile3 = `\
# project: abcdefghijklmn
# version: 3

${modifiedTwitterSchema}`

export const mockedPushSchemaResponseError = `
{
  "data":{
    "migrateProject":{
      "migrationMessages":[
        {
          "name":"Tweet",
          "description":"The type \Tweet\ is removed. This also deletes all its field and relations.",
          "subDescriptions":[

          ],
          "type":"Type",
          "action":"Delete"
        }
      ],
      "errors":[
        {
          "description":"Your migration includes potentially destructive changes. Review using \`graphcool status\` and continue using \`graphcool push --force\`.",
          "type":"Global",
          "field":null
        }
      ],
      "project":{
        "name":"Desertspirit Ninja",
        "alias":null,
        "version":2,
        "id":"cj2j7bpzc05p9011800fq3lxp",
        "schema":"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype Tweet implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  text: String!\\n  updatedAt: DateTime!\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}"
      }
    }
  }
}
`

export const mockedPushSchema1ResponseError = `
{
  "data":{
    "migrateProject":{
      "migrationMessages":[
        {
          "name":"Tweet",
          "description":"The type \Tweet\ is removed. This also deletes all its field and relations.",
          "subDescriptions":[

          ],
          "type":"Type",
          "action":"Delete"
        }
      ],
      "errors":[
        {
          "description":"Your migration includes potentially destructive changes. Review using \`graphcool status\` and continue using \`graphcool push --force\`.",
          "type":"Global",
          "field":null
        }
      ],
      "project":{
        "name":"Desertspirit Ninja",
        "alias":null,
        "version":1,
        "id":"cj2j7bpzc05p9011800fq3lxp",
        "schema":"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype Tweet implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  text: String!\\n  updatedAt: DateTime!\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}"
      }
    }
  }
}
`


export const mockedPushSchemaResponse = `\
{
  "data": {
    "migrateProject": {
      "migrationMessages": [
        {
          "name": "Customer",
          "description": "A new type with the name \`Customer\` is created.",
          "subDescriptions": [
            {
              "type": "Field",
              "action": "Create",
              "name": "name",
              "description": "A new field with the name \`name\` and type \`String\` is created."
            }
          ],
          "type": "Type",
          "action": "Create"
        },
        {
          "name": "Image",
          "description": "A new type with the name \`Image\` is created.",
          "subDescriptions": [
            {
              "type": "Field",
              "action": "Create",
              "name": "url",
              "description": "A new field with the name \`url\` and type \`String\` is created."
            },
            {
              "type": "Field",
              "action": "Create",
              "name": "type",
              "description": "A new field with the name \`type\` and type \`String\` is created."
            },
            {
              "type": "Field",
              "action": "Create",
              "name": "taken",
              "description": "A new field with the name \`taken\` and type \`DateTime\` is created."
            }
          ],
          "type": "Type",
          "action": "Create"
        },
        {
          "name": "TweetsByCustomer",
          "description": "The relation \`TweetsByCustomer\` is created. It connects the type \`Tweet\` with the type \`Customer\`.",
          "subDescriptions": [],
          "type": "Relation",
          "action": "Create"
        },
        {
          "name": "Ball",
          "description": "The relation \`Ball\` is deleted. It connected the type \`User\` with the type \`File\`.",
          "subDescriptions": [],
          "type": "Relation",
          "action": "Delete"
        }
      ],
      "errors": [],
      "project": {
        "version": 3,
        "name": "Example",
        "alias": null,
        "schema": "${modifiedTwitterSchemaJSONFriendly}",
        "id": "cj26898xqm9tz0126n34d64ey"
      }
    }
  }
}
`

export const mockedPullResponseSchema = `\
type File {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n                \\ntype User {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}`

export const mockedPullResponseSchemaWithLineBreaks = `\
type File {
  contentType: String!
  createdAt: DateTime!
  id: ID! @isUnique
  name: String!
  secret: String! @isUnique
  size: Int!
  updatedAt: DateTime!
  url: String! @isUnique
}
                
type User {
  createdAt: DateTime!
  id: ID! @isUnique
  updatedAt: DateTime!
}`

export const mockedPullProjectResponse1 = `\
{
  "data": {
    "viewer": {
      "project": {
        "name": "Solsticeboa Lynx",
        "alias": null,
        "version": 1,
        "id": "cj26898xqm9tz0126n34d64ey",
        "schema": "${mockedPullResponseSchema}",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedPullProjectResponseWithAlias1 = `\
{
  "data": {
    "viewer": {
      "project": {
        "name": "Solsticeboa Lynx",
        "alias": "example",
        "version": 1,
        "id": "cj26898xqm9tz0126n34d64ey",
        "schema": "${mockedPullResponseSchema}",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedPullProjectResponse2 = `\
{
  "data": {
    "viewer": {
      "project": {
        "name": "Solsticeboa Lynx",
        "alias": null,
        "version": 2,
        "id": "cj26898xqm9tz0126n34d64ey",
        "schema": "${mockedPullResponseSchema}",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedPullProjectResponseWithAlias2 = `\
{
  "data": {
    "viewer": {
      "project": {
        "name": "Solsticeboa Lynx",
        "alias": "example",
        "version": 2,
        "id": "cj26898xqm9tz0126n34d64ey",
        "schema": "${mockedPullResponseSchema}",
        "region": "EU_WEST_1"
      }
    }
  }
}`


export const mockedPullProjectFile1 = `\
# project: cj26898xqm9tz0126n34d64ey
# version: 1

${mockedPullResponseSchemaWithLineBreaks}`

export const mockedPullProjectFile2 = `\
# project: cj26898xqm9tz0126n34d64ey
# version: 2

${mockedPullResponseSchemaWithLineBreaks}`


export const mockedPullProjectFileWithAlias1 = `\
# project: example
# version: 1

${mockedPullResponseSchemaWithLineBreaks}`

export const mockedPullProjectFileWithAlias2 = `\
# project: example
# version: 2

${mockedPullResponseSchemaWithLineBreaks}`


export const mockedCreateProjectResponse = `\
{
  "data": {
    "addProject": {
      "project": {
        "id": "abcdefghijklmn",
        "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}\\n",
        "version": 1,
        "alias": null,
        "name": "Example",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedClonedProjectResponse = `\
{
  "data": {
    "cloneProject": {
      "project": {
        "id": "nmlkjihgfedcba",
        "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}\\n",
        "version": 1,
        "alias": null,
        "name": "Clone of Example",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedCreateProjectResponseWithAlias = `\
{
  "data": {
    "addProject": {
      "project": {
        "id": "abcdefghijklmn",
        "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}\\n",
        "alias": "example",
        "version": 1,
        "name": "Example",
        "region": "EU_WEST_1"
      }
    }
  }
}`

export const mockedInvalidSessionPushResponse = `\
{  
  "data":{  
    "migrateProject":null
  },
  "errors":[  
    {  
      "locations":[  
        {  
          "line":2,
          "column":3
        }
      ],
      "path":[  
        "migrateProject"
      ],
      "code":2001,
      "message":"No valid session",
      "requestId":"cj20bqob10nnz0158flvxpedm"
    }
  ]
}`

export const mockedExportLink = `https://s3-eu-west-1.amazonaws.com/graphcool-backend-system-prod-1-data-export/cj27oenv2h2i40115qa7hsigm.zip`

export const mockedExportResponse = `\
{
  "data": {
    "exportData": {
      "url": "${mockedExportLink}"
    }
  }
}`

