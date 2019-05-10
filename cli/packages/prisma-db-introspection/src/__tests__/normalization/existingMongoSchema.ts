import {
  DatabaseType,
  DefaultParser,
  DefaultRenderer,
  dedent,
} from 'prisma-datamodel'
import DefaultNormalizer from '../../common/normalization/defaultNormalizer'

function testWithExisting(schemaFromDb, existingSchema, expectedResultSchema) {
  const parser = DefaultParser.create(DatabaseType.mongo)

  const fromDb = parser.parseFromSchemaString(schemaFromDb)
  const existing = parser.parseFromSchemaString(existingSchema)

  DefaultNormalizer.create(DatabaseType.mongo, existing).normalize(fromDb)

  const renderer = DefaultRenderer.create(DatabaseType.mongo)
  const resultSchema = renderer.render(fromDb)

  expect(resultSchema).toEqual(expectedResultSchema)
}

describe('Schema normalization from existing mongo schema', () => {
  it('Should copy names and directives from an existing schema.', () => {
    const schemaFromDb = `
      type useru {
        age: Int!
        name: String!
        birthdaydate: Date!
        posts: [post]
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
        name: String!
        birthday: Date! @db(name: "birthdaydate")
        signedUp: Date! @createdAt @db(name: "signUpDate")
        posts: [UserPost]
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
        email: String! @id
        name: String!
        age: Int!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should respect the ordering from an existing schema.', () => {
    const schemaFromDb = `
      type User {
        age: Int!
        name: String!
        birthdaydate: Date!
        posts: [Post]
        signUpDate: Date!
      }

      type Post @embedded {
        text: String!
        likes: Int!
        comments: [Comment]
      }
      
      type Comment @embedded {
        text: String!
        likes: Int!
      }
      
      type Vehicle {
        brand: String!
        wheelCount: Int!
        horsePower: Float!
      }
      `

    // User has renamed a few types, but post is missing
    const existingSchema = `
      type Vehicle {
        wheelCount: Int!
        brand: String!
        horsePower: Float!
      }
      
      type User {
        age: Int!
        name: String!
        signUpDate: Date!
      }`

    // The expected result schema
    const expectedResultSchema = dedent(`
      type Vehicle {
        wheelCount: Int!
        brand: String!
        horsePower: Float!
      }
      
      type User {
        age: Int!
        name: String!
        signUpDate: Date!
        birthdaydate: Date!
        posts: [UserPost]
      }

      type UserPost @embedded {
        comments: [UserPostComment]
        likes: Int!
        text: String!
      }
      
      type UserPostComment @embedded {
        likes: Int!
        text: String!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should remove ambiguous relation names.', () => {
    const schemaFromDb = `
      type User {
        email: String! @id
        posts: [Post] @relation(name: "PostToUser")
      }
      
      type Post {
        likes: Int!
        text: String!
        user: User! @relation(name: "PostToUser")
      }`

    const existingSchema = `type User { 
      email: String! @id
    }`

    const expectedResultSchema = dedent(`
      type User {
        email: String! @id
        posts: [Post]
      }
      
      type Post {
        likes: Int!
        text: String!
        user: User!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should not remove non-ambiguous relation names.', () => {
    const schemaFromDb = `
      type User {
        email: String! @id
        likedPosts: [Post] @relation(name: "LikedPosts")
        posts: [Post] @relation(name: "PostToUser")
      }
      
      type Post {
        likes: Int!
        likedBy: [User] @relation(name: "LikedPosts")
        user: User! @relation(name: "PostToUser")
      }`

    const existingSchema = `type User { 
        email: String! @id
      }`

    const expectedResultSchema = dedent(`
      type User {
        email: String! @id
        likedPosts: [Post] @relation(name: "LikedPosts")
        posts: [Post] @relation(name: "PostToUser")
      }
      
      type Post {
        likedBy: [User] @relation(name: "LikedPosts")
        likes: Int!
        user: User! @relation(name: "PostToUser")
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should not remove ambiguous relation names when they are explicitely named in the existing schema.', () => {
    const schemaFromDb = `
      type User {
        email: String! @id
        posts: [Post] @relation(name: "PostToUser")
      }
      
      type Post {
        likes: Int!
        text: String!
        user: User! @relation(name: "PostToUser")
      }`

    const existingSchema = `type User {
      posts: [Post] @relation(name: "PostToUser")
    }
    
    type Post {
      user: User! @relation(name: "PostToUser")
    }`

    const expectedResultSchema = dedent(`
      type User {
        email: String! @id
        posts: [Post] @relation(name: "PostToUser")
      }
      
      type Post {
        user: User! @relation(name: "PostToUser")
        likes: Int!
        text: String!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })

  it('Should remove back relations if they are missing in the reference schema.', () => {
    const schemaFromDb = `
      type User {
        email: String! @id
        posts: [Post]
      }
      
      type Post {
        likes: Int!
        text: String!
        user: User!
      }`

    const existingSchema = `type User {
      posts: [Post!]
    }
    
    type Post {
      likes: Int!
      text: String!
    }`

    const expectedResultSchema = dedent(`
      type User {
        email: String! @id
        posts: [Post]
      }
      
      type Post {
        likes: Int!
        text: String!
      }`)

    testWithExisting(schemaFromDb, existingSchema, expectedResultSchema)
  })
})
