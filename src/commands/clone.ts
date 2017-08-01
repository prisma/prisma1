import {SystemEnvironment, Resolver} from '../types'
import {
  readProjectIdFromProjectFile,
  isValidProjectFilePath, writeProjectFile
} from '../utils/file'
import {
  noProjectIdMessage,
  cloningProjectMessage,
  noProjectFileOrIdMessage,
  invalidProjectFilePathMessage,
  multipleProjectFilesForCloneMessage,
  clonedProjectMessage,
  graphcoolCloneProjectFileName, graphcoolProjectFileName
} from '../utils/constants'
import {
  parseErrors,
  generateErrorOutput,
  pullProjectInfo, cloneProject
} from '../api/api'
const debug = require('debug')('graphcool')

export interface CloneProps {
  sourceProjectId: string
  projectFile?: string
  outputPath?: string
  name?: string
  includeMutationCallbacks?: boolean
  includeData?: boolean
  alias?: string
}

export default async(props: CloneProps, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {

    const projectId = props.sourceProjectId
    out.startSpinner(cloningProjectMessage)

    const includeMutationCallbacks = props.includeMutationCallbacks !== undefined ? props.includeMutationCallbacks : true
    const includeData = props.includeData !== undefined ? props.includeData : true

    const clonedProjectName = await getClonedProjectName(props, projectId, resolver)
    const clonedProjectInfo = await cloneProject(projectId, clonedProjectName, includeMutationCallbacks, includeData, resolver)

    const projectFile = props.projectFile || graphcoolProjectFileName
    const outputPath = props.outputPath || graphcoolCloneProjectFileName(projectFile)

    writeProjectFile(clonedProjectInfo, resolver, outputPath)

    out.stopSpinner()
    const message = clonedProjectMessage(clonedProjectName, outputPath, clonedProjectInfo.projectId)
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

async function getClonedProjectName(props: CloneProps, projectId: string, resolver: Resolver): Promise<string> {
  if (props.name) {
    return Promise.resolve(props.name)
  }

  const projectInfo = await pullProjectInfo(projectId, resolver)
  const clonedPojectName = `${projectInfo.name} copy`
  return clonedPojectName
}
