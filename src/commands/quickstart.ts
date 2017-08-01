import { CheckAuth, SystemEnvironment } from '../types'
import open = require('open')
import { docsEndpoint, openedQuickstartMessage } from '../utils/constants'
import { checkAuth } from '../utils/auth'

const debug = require('debug')('graphcool-auth')

export interface QuickstartProps {
}

export default async (props: QuickstartProps, env: SystemEnvironment): Promise<void> => {
  const alreadyAuthenticated = await checkAuth(env, 'quickstart')

  if (alreadyAuthenticated) {
    open(`${docsEndpoint}/quickstart`)

    env.out.write(openedQuickstartMessage)
  }
}

