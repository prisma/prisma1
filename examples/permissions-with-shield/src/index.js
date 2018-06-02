const {
  GraphQLServer
} = require('graphql-yoga');
const {
  Prisma
} = require('prisma-binding');

const resolvers = require('./resolvers');
const permissions = require('./permissions')
const typeDefs = './src/schema.graphql';

const server = new GraphQLServer({
  typeDefs,
  resolvers,
  resolverValidationOptions: {
    requireResolversForResolveType: false
  },
  middlewares: [permissions],
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: './src/generated/prisma.graphql',
      endpoint: process.env.PRISMA_ENDPOINT,
      debug: true
    })
  })
})

server.start({
  port: process.env.PORT
}, () => console.log(`Server at ${process.env.PORT}`))