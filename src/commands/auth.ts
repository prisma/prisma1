import fetch from 'node-fetch'
import open = require('open')
import cuid = require('cuid')
import ora = require('ora')
import {writeAuthConfig, deleteAuthConfig} from '../utils/file'
import figures = require('figures')
import * as chalk from 'chalk'
import {AuthServer, Resolver, TokenValidationResult} from '../types'
const debug = require('debug')('graphcool')

interface Props {
  token?: string
}

export default async(props: Props, resolver: Resolver, authServer: AuthServer): Promise<void> => {

  let token = props.token!

  if (!token) {
    debug(`No token, access auth server...`)
    console.log()
    const spinner = ora(`Authenticating using your browser...`).start()
    token = await authServer.getAuthToken()
    debug(`Received token: ${token}`)
    spinner.stop()
  }

  debug(`Write token: ${token}`)
  writeAuthConfig({token}, resolver)

  const result = await authServer.validateAuthToken(token)
  debug(`Validation result: ${result}`)
  switch (result) {
    case 'invalid':
      deleteAuthConfig(resolver)
      throw new Error('Invalid auth token')
    case 'valid':
      break
  }

  console.log(`${chalk.green(figures.tick)}  Authenticated successfully`)
}


export class GraphcoolAuthServer implements AuthServer {

  async getAuthToken(): Promise<string> {
    const apiEndpoint = 'https://cli-auth-api.graph.cool'
    const cliToken = cuid()

    await fetch(`${apiEndpoint}/create`, {
      method: 'post',
      body: JSON.stringify({ cliToken }),
    })

    const frontend = 'https://cli-auth.graph.cool'
    open(`${frontend}/?token=${cliToken}`)

    while (true) {
      const result = await fetch(`${apiEndpoint}/${cliToken}`)
      const json = await result.json()
      debug(`Auth token JSON: ${JSON.stringify(json)}`)
      const {authToken} = json
      debug(`Received auth token: ${authToken}`)
      return authToken as string
    }

  }

  async validateAuthToken(token: string): Promise<TokenValidationResult> {

    const authQuery = `{
      viewer {
        user {
          id
        }
      }
    }`

    try {
      await fetch('https://api.graph.cool/system', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: authQuery
      })
    }
    catch(e) {
      return 'invalid'
    }
    return 'valid'

  }


}
