const posts = async (root, args, context, info) => {
  return await context.db.query.posts({}, info);
}

module.exports = {
  posts
}