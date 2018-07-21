const {
  authQueries,
  authMutations,
  getUserId,
} = require('graphql-authentication');

const resolvers = {
  Query: {
    ...authQueries,
    posts: async (root, args, context, info) => {
      return await context.db.query.posts({}, info);
    },
  },
  Mutation: {
    ...authMutations,
    createPost: async (root, args, context, info) => {
      const userId = getUserId(context);
      return await context.db.mutation.createPost(
        {
          data: {
            ...args,
            user: {
              connect: {
                id: userId,
              },
            },
          },
        },
        info
      );
    },
  },
};

module.exports = { resolvers };
