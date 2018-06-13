const { GraphQLServer } = require('graphql-yoga')
const { Prisma } = require('prisma-binding')

const resolvers = require('./resolvers')
const permissions = require('./permissions')
const typeDefs = './src/schema.graphql'

const server = new GraphQLServer({
  typeDefs,
  resolvers,
  resolverValidationOptions: {
    requireResolversForResolveType: false,
  },
  middlewares: [permissions],
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: './src/generated/prisma.graphql',
      endpoint: process.env.PRISMA_ENDPOINT,
      debug: true,
    }),
  }),
})

const port = process.env.PORT || 7200

server.start(
  {
    port,
  },
  () => console.log(`Server at http://localhost:${port}`),
)
