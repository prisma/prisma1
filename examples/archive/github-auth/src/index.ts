import { importSchema } from 'graphql-import'
import { GraphQLServer } from 'graphql-yoga'
import { Prisma } from './generated/prisma'
import { resolvers } from './resolvers'

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      endpoint: process.env.PRISMA_ENDPOINT,  // Prisma service endpoint (see `~/.prisma/config.yml`)
      secret: process.env.PRISMA_SECRET,      // `secret` taken from `prisma.yml`
      debug: true                             // log all requests to the Prisma API to console
    }),
  }),
})

server.start(() => console.log(`Server is running on http://localhost:4000`))