import { file } from './Mutation/file'

export const resolvers = {
  Query: {
    file: async (parent, {id}, context, info) => {
      return context.db.query.file({ where: { id } }, info)
    },
    files: async (parent, args, context, info) => {
      return context.db.query.files(args, info)
    },
  },
  Mutation: {
    ...file,
  }
}
