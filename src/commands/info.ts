import { endpointsMessage, noProjectIdMessage } from '../utils/constants'
import out from '../io/Out'

export interface InfoProps {
  projectId: string
}

export interface InfoCliProps {
  env?: string
}

export default async({projectId}: InfoProps): Promise<void> => {

  if (!projectId) {
    throw new Error(noProjectIdMessage)
  }

  const message = endpointsMessage(projectId)
  out.write(message)
}
