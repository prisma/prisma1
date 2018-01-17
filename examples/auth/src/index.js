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
      typeDefs: 'src/generated/graphcool.graphql',
      endpoint: process.env.GRAPHCOOL_ENDPOINT,
      secret: process.env.GRAPHCOOL_SECRET,
    }),
  }),
})

server.start(({ port }) => console.log(`Server is running on http://localhost:${port}`))
