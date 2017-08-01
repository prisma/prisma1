import { AuthServer, SystemEnvironment } from '../types'
import { sleep } from '../utils/system'
import {
  openBrowserMessage,
  authenticationSuccessMessage,
  couldNotRetrieveTokenMessage,
} from '../utils/constants'

const debug = require('debug')('graphcool-auth')

export interface AuthProps {
  token?: string
}

export default async (props: AuthProps, env: SystemEnvironment, authServer: AuthServer): Promise<void> => {
  const {resolver, out} = env

  let token = props.token!

  if (!token) {
    out.startSpinner(openBrowserMessage)

    await sleep(500)
    try {
      token = await authServer.requestAuthToken()
    } catch (e) {
      debug(e.stack || e)
      throw new Error(couldNotRetrieveTokenMessage)
    }
    out.stopSpinner()
  }

  const authenticatedUserEmail = await authServer.validateAuthToken(token)
  if (authenticatedUserEmail) {
    env.config.set({ token })
    env.config.save()
    out.write(authenticationSuccessMessage(authenticatedUserEmail))
  } else {
    env.config.unset('token')
    env.config.save()
    throw new Error('Invalid auth token')
  }

}

