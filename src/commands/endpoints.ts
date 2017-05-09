import {SystemEnvironment} from '../types'
import {
  readProjectIdFromProjectFile,
  isValidProjectFilePath
} from '../utils/file'
import {
  noProjectIdMessage,
  invalidProjectFilePathMessage,
  noProjectFileOrIdMessage,
  multipleProjectFilesForExportMessage,
  endpointsMessage, noProjectFileMessage
} from '../utils/constants'
const debug = require('debug')('graphcool')

interface Props {
  projectFile?: string
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {

  const {out} = env

  const projectId = getProjectId(props, env)
  if (!projectId) {
    throw new Error(noProjectIdMessage)
  }

  const message = endpointsMessage(projectId)
  out.write(message)
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
    throw new Error(noProjectFileMessage)
  } else if (projectFiles.length > 1) {
    throw new Error(multipleProjectFilesForExportMessage(projectFiles))
  }

  return readProjectIdFromProjectFile(resolver, projectFiles[0])
}
