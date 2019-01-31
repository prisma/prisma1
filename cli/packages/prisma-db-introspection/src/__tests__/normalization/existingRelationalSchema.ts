import { DatabaseType, Parser, DefaultRenderer, dedent } from 'prisma-datamodel'
import DefaultNormalizer from '../../common/normalization/defaultNormalizer'

function testWithExisting(schemaFromDb, existingSchema, expectedResultSchema) {
  const parser = Parser.create(DatabaseType.postgres)

  const fromDb = parser.parseFromSchemaString(schemaFromDb)
  const existing = parser.parseFromSchemaString(existingSchema)

  DefaultNormalizer.create(DatabaseType.postgres, existing).normalize(fromDb)

  const renderer = DefaultRenderer.create(DatabaseType.postgres)
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
      id: Id! @unique
      age: Int!
      name: String!
    }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })
})