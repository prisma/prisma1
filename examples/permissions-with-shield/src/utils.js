const jwt = require('jsonwebtoken');

const ADMIN = 'ADMIN';
const USER = 'USER';
const AUTHOR = 'AUTHOR';
const EDITOR = 'EDITOR';

const availableRoles = [ADMIN, AUTHOR, EDITOR, USER]

const getUserId = (context) => {
  console.log(context.request)
  const Authorization = context.request.get('Authorization');
  if (Authorization) {
    const token = Authorization.replace('Bearer ', '');
    const {
      userId
    } = jwt.verify(token, process.env.APP_SECRET);
    return userId;
  }

  throw new Error('Not authenticated')
}

const createUserRole = async (root, args, context, info) =>
  await context.db.mutation.createUserRole({
    data: {
      user: {
        connect: {
          id: args.userId
        }
      },
      role: {
        connect: {
          id: args.roleId
        }
      }
    }
  }, info)

const findRoleIdByName = async (context, roleName) => {
  const roles = await context.db.query.roles({
    where: {
      name: roleName
    }
  }, `{ id }`)
  return roles[0].id
}

const findUserIdByEmail = async (context, userEmail) => {
  const users = await context.db.query.users({
    where: {
      email: userEmail
    }
  }, `{ id }`)
  return users[0].id
}

const userRoleExists = async (context, args) =>
  await context.db.exists.UserRole({
    user: {
      id: args.userId
    },
    role: {
      id: args.roleId
    }
  })

const titleCase = (str) =>
  str.replace(/\w\S*/g, (txt) => txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase());

module.exports = {
  getUserId,
  ADMIN,
  AUTHOR,
  EDITOR,
  USER,
  availableRoles,
  createUserRole,
  findRoleIdByName,
  findUserIdByEmail,
  userRoleExists,
  titleCase
}