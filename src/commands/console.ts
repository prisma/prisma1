import {readGraphcoolConfig, readProjectIdFromProjectFile, isValidProjectFilePath} from '../utils/file'
import {SystemEnvironment} from '../types'
import open = require('open')
import { pullProjectInfo } from '../api/api'
import {
  consoleURL,
  invalidProjectFilePathMessage,
  notAuthenticatedMessage
} from '../utils/constants'

const debug = require('debug')('graphcool-auth')

interface Props {
  projectFile?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver} = env

  const {token} = readGraphcoolConfig(resolver)
  if (!token) {
    throw new Error(notAuthenticatedMessage)
  }

  const url = await getURL(token, props, env)
  open(url)
}

async function getURL(token: string, props: Props, env: SystemEnvironment): Promise<string> {
  const currentProjectId = getProjectId(props, env)
  if (!currentProjectId) {
    return consoleURL(token)
  }

  const projectInfo = await pullProjectInfo(currentProjectId!, env.resolver)
  return consoleURL(token, projectInfo.name)
}

function getProjectId(props: Props, env: SystemEnvironment): string | undefined {
  const {resolver} = env

  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile  && isValidProjectFilePath(props.projectFile)) {
    return readProjectIdFromProjectFile(resolver, props.projectFile!)
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    throw new Error(invalidProjectFilePathMessage(props.projectFile))
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    return undefined
  } else if (projectFiles.length > 1) {
    return undefined
  }

  return readProjectIdFromProjectFile(resolver, projectFiles[0])
}
