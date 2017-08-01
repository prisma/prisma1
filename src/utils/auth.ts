import { AuthTrigger, SystemEnvironment } from '../types'
import { GraphcoolAuthServer } from '../api/GraphcoolAuthServer'
import authCommand, { AuthProps } from '../commands/auth'

export async function checkAuth(env: SystemEnvironment, authTrigger: AuthTrigger): Promise<boolean> {
  if (env.config.get('token')) {
    return true
  }

  await authCommand({}, env, new GraphcoolAuthServer(authTrigger))
  return false
}
