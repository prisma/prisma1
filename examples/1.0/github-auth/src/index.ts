import { importSchema } from 'graphql-import'
import { GraphQLServer } from 'graphql-yoga'
import { Prisma } from './generated/prisma'

import { resolvers } from './resolvers'

// Config --------------------------------------------------------------------

const APP_SCHEMA_PATH = './src/schema.graphql'

const typeDefs = importSchema(APP_SCHEMA_PATH)

// Server --------------------------------------------------------------------

const server = new GraphQLServer({
  typeDefs,
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      endpoint: process.env.PRISMA_ENDPOINT,
      secret: process.env.PRISMA_SECRET,
    }),
  }),
})

server.start({ port: 5000 }, () => {
  console.log(`Server is running on http://localhost:5000`)
})

// ---------------------------------------------------------------------------
