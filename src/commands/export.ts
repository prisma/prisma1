import {SystemEnvironment, Resolver} from '../types'
import {readProjectIdFromProjectFile, isValidProjectFilePath} from '../utils/file'
import {
  noProjectIdMessage, exportingDataMessage, noProjectFileMessageFound,
  downloadUrlMessage, invalidProjectFilePathMessage, noProjectFileMessage, multipleProjectFilesMessage
} from '../utils/constants'
import {exportProjectData, parseErrors, generateErrorOutput} from '../api/api'
const debug = require('debug')('graphcool')

interface Props {
  sourceProjectId?: string
  projectFile?: string
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {
    out.startSpinner(exportingDataMessage)

    const projectId = props.sourceProjectId || getProjectId(props, env)
    if (!projectId) {
      out.writeError(noProjectIdMessage)
      process.exit(0)
    }

    debug(`Export for project Id: ${projectId}`)
    const url = await exportProjectData(projectId!, resolver)

    const message = downloadUrlMessage(url)
    out.stopSpinner()
    out.write(message)

  } catch(e) {
    out.stopSpinner()
    debug(`Received error: ${JSON.stringify(e)}`)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }

  }

}

function getProjectId(props: Props, env: SystemEnvironment): string | undefined {
  const {resolver, out} = env

  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile  && isValidProjectFilePath(props.projectFile)) {
    return readProjectIdFromProjectFile(resolver, props.projectFile!)
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    out.writeError(invalidProjectFilePathMessage(props.projectFile))
    process.exit(1)
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    out.writeError(noProjectFileMessage)
    process.exit(1)
  } else if (projectFiles.length > 1) {
    out.writeError(multipleProjectFilesMessage(projectFiles))
    process.exit(1)
  }

  return readProjectIdFromProjectFile(resolver, projectFiles[0])
}
