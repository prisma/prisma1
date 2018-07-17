const resolvers = {
  Query: {
    drafts: (_, {}, ctx, info) => {
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
      if (data.status && data.status === 'PUBLISH') {
        throw new Error(`Can't create published posts!`)
      }

      return ctx.db.mutation.createPost({ data }, info)
    },

    deletePost: (_, { id }, ctx, info) => {
      return ctx.db.mutation.deletePost({ where: { id } }, `{ id }`)
    },
  },
}

module.exports = resolvers
