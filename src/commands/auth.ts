import ora = require('ora')
import {writeAuthConfig, deleteAuthConfig} from '../utils/file'
import {AuthServer, Resolver, TokenValidationResult} from '../types'
import {
  openBrowserMessage,
  authenticationSuccessMessage,
  couldNotRetrieveTokenMessage
} from '../utils/constants'
const debug = require('debug')('graphcool-auth')

interface Props {
  token?: string
}

export default async(props: Props, resolver: Resolver, authServer: AuthServer): Promise<void> => {

  let token = props.token!

  if (!token) {
    const spinner = ora(openBrowserMessage).start()
    try {
      token = await authServer.requestAuthToken()
    } catch(e) {
      process.stdout.write(couldNotRetrieveTokenMessage)
      process.exit(1)
    }
    spinner.stop()
  }

  writeAuthConfig({token}, resolver)
  debug(`Did write auth config: ${JSON.stringify(resolver)}`)

  const result = await authServer.validateAuthToken(token)
  debug(`Auth token: ${result}`)
  switch (result) {
    case 'invalid':
      deleteAuthConfig(resolver)
      throw new Error('Invalid auth token')
    case 'valid':
      break
  }

  process.stdout.write(authenticationSuccessMessage)
}

