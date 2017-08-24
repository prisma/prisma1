import open = require('open')
import {
  consoleURL,
  notAuthenticatedMessage, openedConsoleMessage
} from '../utils/constants'
import client from '../io/Client'
const debug = require('debug')('graphcool')

export interface ConsoleProps {
  projectId: string
}

export default async (props: ConsoleProps): Promise<void> => {
  const {resolver, out} = env

  const token = env.config.get('token')
  if (!token) {
    throw new Error(notAuthenticatedMessage)
  }

  const projectInfo = await client.fetchProjectInfo(props.projectId)
  const url = projectInfo ? consoleURL(token, projectInfo.name) : consoleURL(token)

  open(url)
  out.write(openedConsoleMessage(projectInfo ? projectInfo.name : undefined))
}
