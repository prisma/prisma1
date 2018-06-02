const {
  rule,
  and,
  or,
  not,
  shield
} = require('graphql-shield')

const {
  getUserId,
  availableRoles,
  titleCase,
  findRoleIdByName,
  userRoleExists
} = require('../utils')

const otherRules = {
  isPostOwner: rule()(async (root, args, context, info) => {
    const postId = args.id;
    const userId = getUserId(context)
    const post = await context.db.exists.Post({
      id: postId,
      user: {
        id: userId
      }
    })
    return post
  })
}

const genericRules = () => {
  const rulesObj = {};
  availableRoles.forEach(role => {
    rulesObj[`is${titleCase(role)}`] = rule()(async (root, args, context, info) => {
      const userId = getUserId(context);
      const roleId = await findRoleIdByName(context, role);
      const userRoleExistsRes = await userRoleExists(context, {
        userId,
        roleId
      })
      return userRoleExistsRes
    })
  })
  return rulesObj
}

const rules = {
  ...genericRules(),
  ...otherRules
}

const permissions = shield({
  Query: {
    posts: rules.isUser
  },
  Mutation: {
    createPost: or(rules.isAdmin, rules.isAuthor, rules.isEditor),
    updatePost: or(rules.isEditor, rules.isPostOwner),
    assignRole: rules.isAdmin,
    createRole: rules.isAdmin
  }
})

module.exports = permissions