import { AuthServer, SystemEnvironment } from '../types'
import { sleep } from '../utils/system'
import {
  openBrowserMessage,
  authenticationSuccessMessage,
  couldNotRetrieveTokenMessage,
} from '../utils/constants'
import out from '../io/Out'
import config from '../io/config'

const debug = require('debug')('graphcool-auth')

export interface AuthProps {
  token?: string
}

export default async (props: AuthProps, authServer: AuthServer): Promise<void> => {

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
    config.setToken(token)
    config.save()
    out.write(authenticationSuccessMessage(authenticatedUserEmail))
  } else {
    config.unsetToken()
    config.save()
    throw new Error('Invalid auth token')
  }

}

