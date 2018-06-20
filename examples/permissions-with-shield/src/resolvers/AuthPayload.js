const user = async (root, args, context, info) => {
  return await context.db.query.user({
    where: {
      id: root.user.id
    }
  }, info)
}

module.exports = {
  user
}