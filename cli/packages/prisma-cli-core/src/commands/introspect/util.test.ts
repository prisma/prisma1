import { sanitizeMongoUri, populateMongoDatabase } from './util'

test('sanitizeMongoUri', () => {
  expect(sanitizeMongoUri('mongodb://localhost')).toBe(
    'mongodb://localhost/admin',
  )
  expect(sanitizeMongoUri('mongodb://localhost/')).toBe(
    'mongodb://localhost/admin',
  )
  expect(sanitizeMongoUri('mongodb://localhost:27017')).toBe(
    'mongodb://localhost:27017/admin',
  )
  expect(sanitizeMongoUri('mongodb://localhost:27017/')).toBe(
    'mongodb://localhost:27017/admin',
  )
  expect(sanitizeMongoUri('mongodb://localhost:27017/prisma')).toBe(
    'mongodb://localhost:27017/prisma',
  )
  expect(
    sanitizeMongoUri(
      'mongodb+srv://prisma:asdas9djasdpassword@cluster100.mongodb.net/test?retryWrites=true',
    ),
  ).toBe(
    'mongodb+srv://prisma:asdas9djasdpassword@cluster100.mongodb.net/test?retryWrites=true',
  )
})

test('populateMongoDatabase', () => {
  expect(populateMongoDatabase({ uri: 'mongodb://localhost:27017/prisma' }))
    .toMatchInlineSnapshot(`
Object {
  "database": "prisma",
  "uri": "mongodb://localhost:27017/prisma",
}
`)
  expect(
    populateMongoDatabase({
      uri: 'mongodb://localhost:27017/prisma',
      database: 'another-db',
    }),
  ).toMatchInlineSnapshot(`
Object {
  "database": "another-db",
  "uri": "mongodb://localhost:27017/prisma",
}
`)
  expect(
    populateMongoDatabase({
      uri: 'mongodb://localhost:27017/prisma?authSource=admin',
    }),
  ).toMatchInlineSnapshot(`
Object {
  "database": "prisma",
  "uri": "mongodb://localhost:27017/prisma?authSource=admin",
}
`)
  expect(
    populateMongoDatabase({
      uri: 'mongodb://localhost:27017/',
      database: 'database',
    }),
  ).toMatchInlineSnapshot(`
Object {
  "database": "database",
  "uri": "mongodb://localhost:27017/",
}
`)
  expect(() =>
    populateMongoDatabase({
      uri: 'mongodb://localhost:27017/',
    }),
  ).toThrow()
})
