const { GraphQLServer } = require('graphql-yoga')
const { Graphcool } = require('graphcool-binding')

const resolvers = {
  Query: {
    feed(parent, args, ctx, info) {
      return ctx.db.query.posts({ where: { isPublished: true } }, info)
    },
  },
  Mutation: {
    writePost(parent, { title, text, isPublished }, ctx, info) {
      return ctx.db.mutation.createPost({ data: {
          title,
          text,
          isPublished,
        }
      }, info)
    }
  },
  Subscription: {
    publications: {
      subscribe: async(parent, args, ctx, info) => {
        return ctx.db.subscription.post({ where: {
          mutation_in: [CREATED, UPDATED],
          node: {
            isPublished: true
          }
        }}, info)
      },
    },
  }
}

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Graphcool({
      typeDefs: 'src/generated/graphcool.graphql',
      endpoint: 'http://localhost:60000/subscriptions/dev',
      secret: 'mysecret123',
    }),
    debug: true,
  }),
})

server.start(() => console.log('Server is running on http://localhost:4000'))
