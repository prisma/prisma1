import { me } from './Query/me'
import { note } from './Query/note'
import { auth } from './Mutation/auth'
import { notes } from './Mutation/notes'

export const resolvers = {
  Query: {
    me,
    note,
  },
  Mutation: {
    ...auth,
    ...notes
  }
}
