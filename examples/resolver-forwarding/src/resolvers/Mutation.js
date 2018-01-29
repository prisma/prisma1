const { forwardTo } = require('prisma-binding')

// Here you see the difference between forwarding and delegating a mutation
const Mutation = {
  // We are forwarding the `createPost` 1-to-1 from the app to the database API.
  // That's why we use `forwardTo('db')` here.
  // It is called `db` because that's the name of the binding instance that's
  // created in `index.js`.
  createPost: forwardTo('db'),

  // We are transforming the input arguments for the deletePost mutation.
  // That's why we use ctx.db.mutation here.
  deletePost(parent, { id }, ctx, info) {
    return ctx.db.mutation.deletePost({ where: { id } })
  },
}

module.exports = { Mutation }
