const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const {
  getUserId,
  USER,
  findRoleIdByName,
  createUserRole,
  findUserIdByEmail,
  userRoleExists
} = require('../utils')

const createPost = async (root, args, context, info) => {
  const userId = getUserId(context)
  return await context.db.mutation.createPost({
    data: { ...args,
      user: {
        connect: {
          id: userId
        }
      }
    }
  }, info);
}

const updatePost = async (root, args, context, info) => {
  const post = await context.db.mutation.updatePost({
    data: {
      title: args.title,
      content: args.content
    },
    where: {
      id: args.id
    }
  }, info)
  console.log(post)
  return post;
}

const signup = async (root, args, context, info) => {
  const password = await bcrypt.hash(args.password, 10)
  const user = await context.db.mutation.createUser({
    data: { ...args,
      password
    }
  }, `{ id }`);

  const roleId = await findRoleIdByName(context, USER)

  const userRole = await createUserRole(root, {
    userId: user.id,
    roleId
  }, context)

  const token = jwt.sign({
    userId: user.id
  }, process.env.APP_SECRET);

  return {
    token,
    user
  }
}

const login = async (root, args, context, info) => {
  const user = await context.db.query.user({
    where: {
      email: args.email
    }
  }, `{ id password }`)

  if (!user) {
    throw new Error(`No user with the email ${args.email}.`)
  }

  const valid = await bcrypt.compare(args.password, user.password);

  if (!valid) {
    throw new Error(`The password is incorrect.`)
  }

  const token = jwt.sign({
    userId: user.id
  }, process.env.APP_SECRET)

  return {
    token,
    user
  }
}

const createRole = async (root, args, context, info) => await context.db.mutation.createRole({
  data: args
}, info)

const assignRole = async (root, args, context, info) => {
  const roleId = await findRoleIdByName(context, args.role);
  const userId = await findUserIdByEmail(context, args.assigneeEmail);
  const userRoleExistsRes = await userRoleExists(context, {
    userId,
    roleId
  })
  if (userRoleExistsRes) {
    throw new Error(`${args.assigneeEmail} already has ${args.role} rights`)
  }
  return await createUserRole(root, {
    userId,
    roleId
  }, context)
}

module.exports = {
  createPost,
  signup,
  login,
  assignRole,
  createRole,
  updatePost
}