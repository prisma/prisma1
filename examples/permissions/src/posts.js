const { getUserId } = require('./utils')

async function createPost(parent, { title }, ctx, info) {
  const userId = getUserId(ctx)
  return await ctx.db.mutation.createPost({
    data: {
      title,
      author: {
        connect: {
          id: userId,
        },
      },
    },
  })
}

async function updatePost(parent, { id, title }, ctx, info) {
  const userId = getUserId(ctx)
  const requestingUserIsAuthor = await ctx.db.exists.posts({
    id,
    author: {
      id: userId,
    },
  })
  const requestingUserIsAdmin = await ctx.db.exists.users({
    id: userId,
    role: 'ADMIN',
  })

  if (requestingUserIsAdmin || requestingUserIsAuthor) {
    return await ctx.db.mutation.updatePost({
      where: { id },
      data: { title },
    })
  }
  throw new Error(
    'Invalid permissions, you must be an admin or the author of a post to update it',
  )
}

async function deletePost(parent, { id }, ctx, info) {
  const userId = getUserId(ctx)
  const requestingUserIsAuthor = await ctx.db.exists.posts({
    id,
    author: {
      id: userId,
    },
  })
  const requestingUserIsAdmin = await ctx.db.exists.users({
    id: userId,
    role: 'ADMIN',
  })

  if (requestingUserIsAdmin || requestingUserIsAuthor) {
    return await ctx.db.mutation.deletePost({
      where: { id },
    })
  }
  throw new Error(
    'Invalid permissions, you must be an admin or the author of a post to delete it',
  )
}

async function posts(parent, args, ctx, info) {
  const userId = getUserId(ctx)
  if (!userId) {
    throw new Error('You must be authenticated to view the posts.')
  }
  return await ctx.db.query.posts
}

module.exports = {
  createPost,
  updatePost,
  deletePost,
  posts,
}
