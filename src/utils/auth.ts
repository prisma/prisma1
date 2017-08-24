import { AuthTrigger } from '../types'
import { GraphcoolAuthServer } from '../io/GraphcoolAuthServer'
import authCommand, { AuthProps } from '../commands/auth'
import config from './config'

export async function checkAuth(authTrigger: AuthTrigger): Promise<boolean> {
  if (config.token) {
    return true
  }

  await authCommand({}, new GraphcoolAuthServer(authTrigger))
  return false
}
