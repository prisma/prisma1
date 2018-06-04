const { GraphQLServer } = require('graphql-yoga')
const { Prisma } = require('prisma-binding')

const getPrismaInstance = () => {
  return new Prisma({
    typeDefs: 'generated-schema.graphql',
    endpoint: 'http://localhost:4466/application-server',
  })
}

const resolvers = {
  Query: {
    drafts: (
      _,
      { where, orderBy, skip, after, before, first, last },
      ctx,
      info,
    ) => {
      return ctx.db.query.posts(
        {
          where: {
            status: 'DRAFT',
          },
        },
        info,
      )
    },
  },

  Mutation: {
    createPost: (_, { data }, ctx, info) => {
      return ctx.db.mutation.createPost({ data }, info)
    },

    deletePost: (_, { id }, ctx, info) => {
      return ctx.db.mutation.deletePost({ where: { id } }, `{ id }`)
    },
  },
}

const server = new GraphQLServer({
  typeDefs: 'schema.graphql',
  resolvers,
  context: {
    db: getPrismaInstance(),
  },
})
server.start(() => console.log('Server is running on localhost:4000'))
