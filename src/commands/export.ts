import {SystemEnvironment} from '../types'
import {
  readProjectIdFromProjectFile,
  isValidProjectFilePath
} from '../utils/file'
import {
  noProjectIdMessage,
  exportingDataMessage,
  downloadUrlMessage,
  invalidProjectFilePathMessage,
  multipleProjectFilesForExportMessage,
  noProjectFileMessage
} from '../utils/constants'
import {
  exportProjectData,
  parseErrors,
  generateErrorOutput
} from '../api/api'
const debug = require('debug')('graphcool')

export interface ExportProps {
  projectFile?: string
}

export default async(props: ExportProps, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {

    const projectId = getProjectId(props, env)
    if (!projectId) {
      throw new Error(noProjectIdMessage)
    }

    out.startSpinner(exportingDataMessage)

    const url = await exportProjectData(projectId!, resolver)

    const message = downloadUrlMessage(url)
    out.stopSpinner()
    out.write(message)

  } catch(e) {
    out.stopSpinner()
    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }

  }

}

function getProjectId(props: ExportProps, env: SystemEnvironment): string | undefined {
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
