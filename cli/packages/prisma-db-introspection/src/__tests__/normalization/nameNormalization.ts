import {
  DatabaseType,
  DefaultParser,
  DefaultRenderer,
  dedent,
} from 'prisma-datamodel'
import ModelNameAndDirectiveNormalizer from '../../common/normalization/modelNameAndDirectiveNormalizer'

function testNormalization(schemaFromDb, expectedResultSchema) {
  const parser = DefaultParser.create(DatabaseType.mongo)

  const fromDb = parser.parseFromSchemaString(schemaFromDb)

  const normalizer = new ModelNameAndDirectiveNormalizer(null)

  normalizer.normalize(fromDb)

  const renderer = DefaultRenderer.create(DatabaseType.mongo)
  const resultSchema = renderer.render(fromDb, true)

  expect(resultSchema).toEqual(expectedResultSchema)
}

describe('Schema normalization from database schema', () => {
  it('Should normalize type names.', () => {
    const schemaFromDb = `type user {
        age: Int!
        name: String!
        birthday: Date!
        posts: [post]
        signedUp: Date!
      }

      type post @embedded {
        text: String!
        likes: Int!
      }`

    // The expected result schema
    const expectedResultSchema = dedent(`
      type User @db(name: "user") {
        age: Int!
        birthday: Date!
        name: String!
        posts: [UserPost]
        signedUp: Date!
      }

      type UserPost @embedded {
        likes: Int!
        text: String!
      }`)

    testNormalization(schemaFromDb, expectedResultSchema)
  })

  it('Should correctly name nested embedded types.', () => {
    const schemaFromDb = `  
      type post @embedded {
        text: String!
        comments: [comment]
      }
      
      type comment @embedded {
        text: String!
      }

      type user {
        name: String!
        posts: [post]
      }`

    const expectedResultSchema = dedent(`
      type User @db(name: "user") {
        name: String!
        posts: [UserPost]
      }

      type UserPost @embedded {
        comments: [UserPostComment]
        text: String!
      }
      
      type UserPostComment @embedded {
        text: String!
      }`)

    testNormalization(schemaFromDb, expectedResultSchema)
  })
})
