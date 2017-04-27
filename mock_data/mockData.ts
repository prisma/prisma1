/*
 * Testing
 */
export const testToken = 'abcdefghijklmnopqrstuvwxyz'

export const mockFullSchema = `\
type Tweet {
  id: ID!
  createdAt: DateTime!
  updatedAt: DateTime!
  text: String!
}`

export const mockSchema1 = `\
type Tweet {
  text: String!
}`

export const mockSchema2 = `\
type Tweet {
  text: String!
  author: Customer! @relation(name: "TweetsByCustomer")
}

type Customer {
  name: String!
  tweets: [Tweet!]! @relation(name: "TweetsByCustomer")
}

type Image {
  url: String!
  type: String
  taken: DateTime
}
`

export const mockProjectFile1 = `\
# projectId: abcdefghijklmn
# version: 1

${mockFullSchema}
`

export const mockProjectFile2 = `\
# projectId: abcdefghijklmn
# version: 2

${mockSchema2}
`

export const mockProjectFile3 = `\
# projectId: abcdefghijklmn
# version: 3

${mockSchema2}
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
        "version": "3"
      }
    }
  }
}
`

export const mockedCreateProjectResponse = `\
{
  "data": {
    "addProject": {
      "project": {
        "id": "abcdefghijklmn",
        "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}\\n"
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