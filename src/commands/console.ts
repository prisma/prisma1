import {readProjectIdFromProjectFile, isValidProjectFilePath} from '../utils/file'
import {SystemEnvironment, ProjectInfo} from '../types'
import open = require('open')
import { pullProjectInfo } from '../api/api'
import {
  consoleURL,
  invalidProjectFilePathMessage,
  notAuthenticatedMessage, openedConsoleMessage
} from '../utils/constants'
const debug = require('debug')('graphcool')

export interface ConsoleProps {
  projectFile?: string
}

export default async (props: ConsoleProps, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  const token = env.config.get('token')
  if (!token) {
    throw new Error(notAuthenticatedMessage)
  }

  const projectInfo = await getProjectInfo(token, props, env)
  const url = projectInfo ? consoleURL(token, projectInfo.name) : consoleURL(token)

  open(url)
  out.write(openedConsoleMessage(projectInfo ? projectInfo.name : undefined))
}


async function getProjectInfo(token: string, props: ConsoleProps, env: SystemEnvironment): Promise<ProjectInfo | undefined> {
  const currentProjectId = getProjectId(props, env)
  if (!currentProjectId) {
    return undefined
  }

  const projectInfo = await pullProjectInfo(currentProjectId!, env.resolver)
  return projectInfo
}

function getProjectId(props: ConsoleProps, env: SystemEnvironment): string | undefined {
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
