const { GraphQLServer } = require('graphql-yoga')

const resolvers = require('./resolvers')

const { Prisma } = require('prisma-binding')
const getPrismaInstance = () => {
  return new Prisma({
    typeDefs: './server/generated-schema.graphql',
    endpoint: 'http://localhost:4466/travis-jest/test',
    secret: 'my-very-secret',
  })
}

const server = new GraphQLServer({
  typeDefs: 'schema.graphql',
  resolvers,
  context: {
    db: getPrismaInstance(),
  },
})
server.start(() => console.log('Server is running on http://localhost:4000'))
