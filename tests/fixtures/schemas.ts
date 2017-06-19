export const simpleTwitterSchema = `\
type Tweet {
  text: String!
}`

export const simpleTwitterSchemaWithSystemFields = `\
type Tweet {
  id: ID!
  createdAt: DateTime!
  updatedAt: DateTime!
  text: String!
}`


export const modifiedTwitterSchema = `\
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
}`

export const modifiedTwitterSchemaJSONFriendly = `type Tweet {\\n  text: String!\\n  author: Customer! @relation(name: \\"TweetsByCustomer\\")\\n}\\n\\ntype Customer {\\n  name: String!\\n  tweets: [Tweet!]! @relation(name: \\"TweetsByCustomer\\")\\n}\\n\\ntype Image {\\n  url: String!\\n  type: String\\n  taken: DateTime\\n}`