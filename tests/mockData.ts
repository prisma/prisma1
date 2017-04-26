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

export const mockedPushSchemaResponse = `\
{
  "migrateProject": {
    ""
  }
}
`

export const mockedCreateProjectResponse = `\
{
  "addProject": {
    "project": {
      "id": "abcdefghi",
      "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}"
    }
  }
}`