import { writeGraphcoolConfig, deleteGraphcoolConfig } from '../utils/file'
import { AuthServer, SystemEnvironment } from '../types'
import { sleep } from '../utils/system'
import {
  openBrowserMessage,
  authenticationSuccessMessage,
  couldNotRetrieveTokenMessage,
} from '../utils/constants'

const debug = require('debug')('graphcool-auth')

interface Props {
  token?: string
}

export default async (props: Props, env: SystemEnvironment, authServer: AuthServer): Promise<void> => {
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
    writeGraphcoolConfig({token}, resolver)
    out.write(authenticationSuccessMessage(authenticatedUserEmail))
  } else {
    deleteGraphcoolConfig(resolver)
    throw new Error('Invalid auth token')
  }

}

