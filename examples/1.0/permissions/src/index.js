const { GraphQLServer } = require('graphql-yoga')
const { Prisma } = require('prisma-binding')
const { me, signup, login, updatePassword, AuthPayload } = require('./auth')
const { createPost, updatePost, deletePost, posts } = require('./posts')
const { user } = require('./users')

const resolvers = {
  Query: {
    me,
    posts,
    user,
  },
  Mutation: {
    signup,
    login,
    updatePassword,
    createPost,
    updatePost,
    deletePost,
  },
}

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: process.env.PRISMA_ENDPOINT,
      secret: process.env.PRISMA_SECRET,
      debug: true,
    }),
  }),
})

server.start(({ port }) => console.log(`Server is running on http://localhost:${port}`))
