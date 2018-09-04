import test from 'ava'
import { Prisma } from './Prisma'
import { join } from 'path'

test('multiple Prisma instances with unique schemas do not share schemas', t => {
  const prismaA = new Prisma({
    typeDefs: join(__dirname, '../src/fixtures/testSchemaA.graphql'),
    endpoint: 'https://mock-prisma-endpoint.io/serviceA',
    secret: 'secretA',
  })

  const prismaB = new Prisma({
    typeDefs: join(__dirname, '../src/fixtures/testSchemaB.graphql'),
    endpoint: 'https://mock-prisma-endpoint.io/serviceB',
    secret: 'secretB',
  })

  t.not(prismaA.schema, prismaB.schema)
})

test('multiple Prisma instances with the same schema use a cached copy', t => {
  const options = {
    typeDefs: join(__dirname, '../src/fixtures/testSchemaA.graphql'),
    endpoint: 'https://mock-prisma-endpoint.io/serviceA',
    secret: 'secretA',
  }

  const prismaA = new Prisma(options)
  const prismaB = new Prisma(options)

  t.is(prismaA.schema, prismaB.schema)
})
