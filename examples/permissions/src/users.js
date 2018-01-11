const { getUserId } = require('./utils')

async function user(parent, { id }, ctx, info) {
  const requestingUserId = getUserId(ctx)
  return await ctx.db.query.user({ id })
}

module.exports = {
  user,
}
