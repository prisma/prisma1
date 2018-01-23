const { GraphQLServer } = require('graphql-yoga')
const { importSchema } = require('graphql-import')
const { Prisma } = require('prisma-binding')
const { me, signup, login, AuthPayload } = require('./auth')

const resolvers = {
  Query: {
    me,
  },
  Mutation: {
    signup,
    login,
  },
}

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',             // points to Prisma database schema
      endpoint: 'http://localhost:4420/auth-example/dev',   // (local) endpoint for Prisma service
      secret: 'mysecret123',                                // `secret` taken from `prisma.yml`
    }),
  }),
})

server.start(() => console.log(`Server is running on http://localhost:4000`))
