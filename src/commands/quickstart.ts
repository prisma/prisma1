import { CheckAuth, SystemEnvironment } from '../types'
import open = require('open')
import { docsEndpoint, openedQuickstartMessage } from '../utils/constants'

const debug = require('debug')('graphcool-auth')

interface Props {
  checkAuth: CheckAuth
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const alreadyAuthenticated = await props.checkAuth(env, 'quickstart')

  if (alreadyAuthenticated) {
    open(`${docsEndpoint}/quickstart`)

    env.out.write(openedQuickstartMessage)
  }
}

