import { DatabaseType, Parser, DefaultRenderer, dedent } from 'prisma-datamodel'
import ModelNameAndDirectiveNormalizer from '../../common/modelNameAndDirectiveNormalizer'

function testWithExisting(schemaFromDb, existingSchema, expectedResultSchema) {
  const parser = Parser.create(DatabaseType.mongo)

  const fromDb = parser.parseFromSchemaString(schemaFromDb)
  const existing = parser.parseFromSchemaString(existingSchema)

  const normalizer = new ModelNameAndDirectiveNormalizer(existing)

  normalizer.normalize(fromDb)

  const renderer = DefaultRenderer.create(DatabaseType.mongo)
  const resultSchema = renderer.render(fromDb)

  expect(resultSchema).toEqual(expectedResultSchema)
}

describe('Schema normalization from existing schema', () => {
  it('Should copy names and directives from an existing schema.', () => {

    const schemaFromDb = `
      type useru {
        age: Int!
        name: String!
        birthdaydate: Date!
        posts: [post!]!
        signUpDate: Date!
      }

      type post @embedded {
        text: String!
        likes: Int!
      }`

    // User has renamed a few types, but post is missing 
    const existingSchema = `
      type User @db(name: "useru") {
        age: Int!
        name: String!
        birthday: Date! @db(name: "birthdaydate")
        signedUp: Date! @db(name: "signUpDate") @createdAt
      }`

    // The expected result schema
    const expectedResultSchema = dedent(`
      type User @db(name: "useru") {
        age: Int!
        birthday: Date! @db(name: "birthdaydate")
        name: String!
        posts: [UserPost!]!
        signedUp: Date! @createdAt @db(name: "signUpDate")
      }

      type UserPost @embedded {
        likes: Int!
        text: String!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })


  it('Should identify ID field correctly.', () => {
    const schemaFromDb = `
      type useru {
        _id: String! @id
        name: String!
        age: Int!
      }`

    // User has a renamed ID field
    const existingSchema = `
      type User @db(name: "useru") {
        email: String! @id
        name: String!
      }`

    const expectedResultSchema = dedent(`
      type User @db(name: "useru") {
        age: Int!
        email: String! @id
        name: String!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })
})