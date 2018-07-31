const { GraphQLServer } = require('graphql-yoga')

const resolvers = require('./resolvers')

const { Prisma } = require('prisma-binding')

const server = new GraphQLServer({
  typeDefs: 'src/schema/schema.graphql',
  resolvers,
  context: {
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: 'http://localhost:4466/travis-jest/test',
      secret: 'my-very-secret',
    }),
  },
})
server.start(() => console.log('Server is running on http://localhost:4000'))
