const { GraphQLServer } = require('graphql-yoga')
const { Prisma } = require('prisma-binding')

const resolvers = {
  Query: {
    feed(parent, args, ctx, info) {
      return ctx.db.query.posts({}, info)
    },
  },
  Mutation: {
    writePost(parent, { title }, ctx, info) {
      return ctx.db.mutation.createPost(
        {
          data: {
            title,
          },
        },
        info,
      )
    },
    updateTitle(parent, { newTitle }, ctx, info) {
      return ctx.db.mutation.updatePost(
        {
          data: {
            title: newTitle,
          },
        },
        info,
      )
    },
  },
  Subscription: {
    publications: {
      subscribe: async (parent, args, ctx, info) => {
        return ctx.db.subscription.post(
          {
            where: {
              mutation_in: ['CREATED', 'UPDATED'],
            },
          },
          info,
        )
      },
    },
  },
}

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: 'http://localhost:4466/subscriptions-example/dev',
      secret: 'mysecret123',
    }),
    debug: true,
  }),
})

server.start(() => console.log(`Server is running on http://localhost:4000`))
