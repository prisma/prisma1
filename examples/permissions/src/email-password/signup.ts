import { fromEvent } from 'graphcool-lib'
import { GraphQLClient } from 'graphql-request'
import * as bcrypt from 'bcrypt'
import * as validator from 'validator'

interface User {
  id: string
}

interface EventData {
  email: string
  password: string
}

export default async (event: FunctionEvent<EventData>) => {
  if (!event.context.graphcool.token) {
    return { error: 'Function not configured correctly - needs token.' }
  }

  const { email, password } = event.data
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')
  const SALT_ROUNDS = 10

  if (!validator.isEmail(email)) {
    return { error: 'Not a valid email' }
  }

  const userExists = await api.request<{ User }>(`{ User(email: "${email}") { id } }`).then(r => r.User !== null)
  if (userExists !== null) {
    return { error: 'Email already in use' }
  }

  const hash = await bcrypt.hash(password, SALT_ROUNDS)
  const graphcoolUserId = await createGraphcoolUser(api, email, hash)
  const token = await graphcool.generateNodeToken(graphcoolUserId, 'User')

  return { data: { id: graphcoolUserId, token } }
}

async function createGraphcoolUser(api: GraphQLClient, email: string, passwordHash: string): Promise<string> {
  const query = `
    mutation {
      createUser(
        email: "${email}",
        password: "${passwordHash}"
      ) {
        id
      }
    }
  `
  return api.request<{ createUser: User }>(query).then(r => r.createUser.id)
}
