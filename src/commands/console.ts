import { readGraphcoolConfig, readProjectIdFromProjectFile } from '../utils/file'
import { SystemEnvironment } from '../types'
import open = require('open')
import { pullProjectInfo } from '../api/api'
import { noProjectFileMessageFound } from '../utils/constants'

const debug = require('debug')('graphcool-auth')

interface Props {
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  const currentProjectId = readProjectIdFromProjectFile(resolver)

  if (!currentProjectId) {
    out.writeError(noProjectFileMessageFound)
    process.exit(1)
  }

  const {name} = await pullProjectInfo(currentProjectId!, resolver)
  const {token} = readGraphcoolConfig(resolver)

  open(`https://console.graph.cool/token/?token=${token}`)

}

