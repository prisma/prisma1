import { notAuthenticatedMessage, openedPlaygroundMessage, playgroundURL, } from '../utils/constants'
import client from '../io/Client'
import out from '../io/Out'
import config from '../io/GraphcoolRC'
import open = require('open')

export interface PlaygroundProps {
  projectId: string
}

export default async ({projectId}: PlaygroundProps): Promise<void> => {
  const {token} = config
  if (!token) {
    throw new Error(notAuthenticatedMessage)
  }

  const name = await client.getProjectName(projectId)
  const url = playgroundURL(token, name)
  open(url)
  out.write(openedPlaygroundMessage(name))
}
