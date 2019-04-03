import {
  DatabaseType,
  DefaultParser,
  DefaultRenderer,
  dedent,
} from 'prisma-datamodel'
import DefaultNormalizer from '../../common/normalization/defaultNormalizer'

function testWithExisting(schemaFromDb, existingSchema, expectedResultSchema) {
  const parser = DefaultParser.create(DatabaseType.postgres)

  const fromDb = parser.parseFromSchemaString(schemaFromDb)
  const existing = parser.parseFromSchemaString(existingSchema)

  DefaultNormalizer.create(DatabaseType.postgres, existing).normalize(fromDb)

  const renderer = DefaultRenderer.create(DatabaseType.postgres, true)
  const resultSchema = renderer.render(fromDb)

  expect(resultSchema).toEqual(expectedResultSchema)
}

describe('Schema normalization from existing postgres schema', () => {
  it('Should hide reserved fields when they are not existing in the ref schema.', () => {
    const schemaFromDb = `
      type User {
        id: Id! @unique
        age: Int!
        name: String!
        createdAt: DateTime!
        updatedAt: DateTime!
      }`

    // User has renamed a few types, but post is missing
    const existingSchema = `type User {
      id: Id! @unique
      age: Int!
      name: String!
    }`

    // The expected result schema
    const expectedResultSchema = dedent(`
    type User {
      id: Id! @id
      age: Int!
      name: String!
      createdAt: DateTime! @createdAt
      updatedAt: DateTime! @updatedAt
    }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should adjust n:n relations and fall back to 1:n if a 1:n relation is given in the reference datamodel.', () => {
    const schemaFromDb = `
      type User {
        id: Id! @unique
        posts: [Post]
      }
      
      type Post {
        id: Id! @unique
        text: String!
        user: [User]
      }`

    // User has renamed a few types, but post is missing
    const existingSchema = `
      type User {
        id: Id! @unique
        posts: [Post]
      }
      
      type Post {
        id: Id! @unique
        text: String!
        user: User!
      }`

    // The expected result schema
    const expectedResultSchema = dedent(`
      type User {
        id: Id! @id
        posts: [Post]
      }
      
      type Post {
        id: Id! @id
        text: String!
        user: User! @relation(link: TABLE)
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should adjust n:n relations and fall back to 1:1 if a 1:1 relation is given in the reference datamodel.', () => {
    const schemaFromDb = `
      type User {
        id: Id! @unique
        posts: [Post]
      }
      
      type Post {
        id: Id! @unique
        text: String!
        user: [User]
      }`

    // User has renamed a few types, but post is missing
    const existingSchema = `
      type User {
        id: Id! @unique
        posts: Post!
      }
      
      type Post {
        id: Id! @unique
        text: String!
        user: User!
      }`

    // The expected result schema
    const expectedResultSchema = dedent(`
      type User {
        id: Id! @id
        posts: Post! @relation(link: TABLE)
      }
      
      type Post {
        id: Id! @id
        text: String!
        user: User!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })
})
