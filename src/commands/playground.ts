import {readProjectIdFromProjectFile, isValidProjectFilePath} from '../utils/file'
import {SystemEnvironment, ProjectInfo} from '../types'
import { pullProjectInfo } from '../api/api'
import open = require('open')
import {
  invalidProjectFilePathMessage,
  notAuthenticatedMessage,
  playgroundURL,
  tooManyProjectFilesForPlaygroundMessage,
  openedPlaygroundMessage,
  canNotReadProjectIdFromProjectFile
} from '../utils/constants'

const debug = require('debug')('graphcool-auth')

export interface PlaygroundProps {
  projectFile?: string
}

export default async (props: PlaygroundProps, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  const token = env.config.get('token')
  if (!token) {
    throw new Error(notAuthenticatedMessage)
  }

  const projectInfo = await getProjectInfo(props, env)
  const url = playgroundURL(token, projectInfo.name)
  open(url)
  out.write(openedPlaygroundMessage(projectInfo.name))
}

async function getProjectInfo(props: PlaygroundProps, env: SystemEnvironment): Promise<ProjectInfo> {
  const currentProjectId = getProjectId(props, env)
  if (!currentProjectId) {
    throw new Error(canNotReadProjectIdFromProjectFile)
  }

  const projectInfo = await pullProjectInfo(currentProjectId!, env.resolver)
  return projectInfo
}

function getProjectId(props: PlaygroundProps, env: SystemEnvironment): string | undefined {
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
    throw new Error(tooManyProjectFilesForPlaygroundMessage(projectFiles))
  }

  return readProjectIdFromProjectFile(resolver, projectFiles[0])
}
