import { readGraphcoolConfig, readProjectIdFromProjectFile } from '../utils/file'
import {SystemEnvironment, Resolver} from '../types'
import open = require('open')
import { pullProjectInfo } from '../api/api'
import {
  graphcoolConfigFilePath,
  graphcoolProjectFileName,
  noProjectFileOrIdMessage
} from '../utils/constants'

const debug = require('debug')('graphcool-auth')

interface Props {
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver} = env

  const currentProjectId = getCurrentProjectId(resolver)

  if (!currentProjectId) {
    throw new Error(noProjectFileOrIdMessage)
  }

  await pullProjectInfo(currentProjectId!, resolver)
  const {token} = readGraphcoolConfig(resolver)

  open(`https://console.graph.cool/token/?token=${token}/Freecom`)

}

function getCurrentProjectId(resolver: Resolver): string | undefined {
  if (resolver.exists(graphcoolConfigFilePath)) {
    return readProjectIdFromProjectFile(resolver, graphcoolProjectFileName)
  }
  return undefined
}