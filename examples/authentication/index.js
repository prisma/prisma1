const { GraphQLServer } = require('graphql-yoga')
const { Prisma } = require('prisma-binding')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const { Context, getUserId, APP_SECRET } = require('./utils')

const getPrismaInstance = () => {
  return new Prisma({
    typeDefs: 'generated-schema.graphql',
    endpoint: 'http://localhost:4466/authentication',
  })
}

const resolvers = {
  Query: {
    me: (parent, args, ctx, info) => {
      const id = getUserId(ctx)
      return ctx.db.query.user({ where: { id } }, info)
    },
  },

  Mutation: {
    signup: async (parent, args, ctx, info) => {
      const password = await bcrypt.hash(args.password, 10)
      const user = await ctx.db.mutation.createUser({
        data: { ...args, password },
      })

      return {
        token: jwt.sign({ userId: user.id }, APP_SECRET),
        user,
      }
    },
    login: async (parent, { email, password }, ctx, info) => {
      const user = await ctx.db.query.user({ where: { email } })
      if (!user) {
        throw new Error(`No user found for email: ${email}`)
      }

      const valid = await bcrypt.compare(password, user.password)
      if (!valid) {
        throw new Error('Invalid password')
      }

      return {
        token: jwt.sign({ userId: user.id }, APP_SECRET),
        user,
      }
    },
  },
  AuthPayload: {
    user: async ({ user: { id } }, args, ctx, info) => {
      return ctx.db.query.user({ where: { id } }, info)
    },
  },
}

const server = new GraphQLServer({
  typeDefs: 'schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: getPrismaInstance(),
  }),
})
server.start(() => console.log('Server is running on localhost:4000'))
