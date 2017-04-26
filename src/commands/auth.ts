import {writeGraphcoolConfig, deleteGraphcoolConfig} from '../utils/file'
import {AuthServer, SystemEnvironment} from '../types'
import {
  openBrowserMessage,
  authenticationSuccessMessage,
  couldNotRetrieveTokenMessage
} from '../utils/constants'
const debug = require('debug')('graphcool-auth')

interface Props {
  token?: string
}

export default async(props: Props, env: SystemEnvironment, authServer: AuthServer): Promise<void> => {

  const {resolver, out} = env

  let token = props.token!

  if (!token) {
    out.startSpinner(openBrowserMessage)
    try {
      token = await authServer.requestAuthToken()
    } catch(e) {
      out.write(couldNotRetrieveTokenMessage)
      process.exit(1)
    }
    out.stopSpinner()
  }

  writeGraphcoolConfig({token}, resolver)

  const result = await authServer.validateAuthToken(token)
  switch (result) {
    case 'invalid':
      deleteGraphcoolConfig(resolver)
      throw new Error('Invalid auth token')
    case 'valid':
      break
  }

  out.write(authenticationSuccessMessage)
}

