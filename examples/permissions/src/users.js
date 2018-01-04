const { getUserId } = require('./utils')

async function user(parent, { id }, ctx, info) {
  const requestingUserId = getUserId(ctx)
  const requestingUserIsAdmin = ctx.db.exists.users({
    id: requestingUserId,
    role: 'ADMIN',
  })
  return await ctx.db.query.user({ id })
}

module.exports = {
  user,
}
