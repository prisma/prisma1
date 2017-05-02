import {SystemEnvironment, Resolver} from '../types'
import {
  readProjectIdFromProjectFile, writeProjectFile, readVersionFromProjectFile,
  isValidProjectFilePath
} from '../utils/file'
import { pullProjectInfo, parseErrors, generateErrorOutput } from '../api/api'
import figures = require('figures')
import {
  fetchingProjectDataMessage,
  noProjectIdMessage,
  wroteProjectFileMessage,
  newVersionMessage, differentProjectIdWarningMessage, invalidProjectFilePathMessage, noProjectFileMessage,
  multipleProjectFilesMessage, graphcoolProjectFileName, multipleProjectFilesForPullMessage
} from '../utils/constants'
var term = require( 'terminal-kit' ).terminal

const debug = require('debug')('graphcool')

interface Props {
  sourceProjectId?: string
  projectFile?: string
  outputPath?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {

    const projectId = getProjectId(props, env)
    const projectFile = props.projectFile || graphcoolProjectFileName
    const currentVersion = readVersionFromProjectFile(resolver, projectFile)

    if (!projectId) {
      throw new Error(noProjectIdMessage)
    }

    // warn if the current project file is different from specified project id
    const readProjectId = readProjectIdFromProjectFile(resolver, graphcoolProjectFileName)
    if (readProjectId && projectId !== readProjectId) {
      out.write(differentProjectIdWarningMessage(projectId!, readProjectId))
      term.grabInput(true)

      await new Promise(resolve => {
        term.on('key' , function(name) {
          if (name !== 'y') {
            process.exit(0)
          }
          term.grabInput(false)
          resolve()
        })
      })
    }

    out.startSpinner(fetchingProjectDataMessage)
    const projectInfo = await pullProjectInfo(projectId!, resolver)
    debug(`Project Info: \n${JSON.stringify(projectInfo)}`)

    out.stopSpinner()
    writeProjectFile(projectInfo, resolver, props.outputPath)

    out.write(wroteProjectFileMessage)
    if (projectInfo.version && currentVersion) {
      const shouldDisplayVersionUpdate = parseInt(projectInfo.version!) > parseInt(currentVersion!)
      if (shouldDisplayVersionUpdate) {
        const message = newVersionMessage(projectInfo.version)
        out.write(` ${message}`)
      }
    }

  } catch (e) {
    out.stopSpinner()

    debug(`${JSON.stringify(e)}`)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }

  }

}

function getProjectFilePath(props: Props, env: SystemEnvironment): string {
  const {resolver, out} = env

  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile && isValidProjectFilePath(props.projectFile)) {
    return props.projectFile
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    throw new Error(invalidProjectFilePathMessage(props.projectFile))
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    throw new Error(noProjectFileMessage)
  } else if (projectFiles.length > 1) {
    throw new Error(multipleProjectFilesForPullMessage(projectFiles))
  }

  return projectFiles[0]
}

function getProjectId(props: Props, env: SystemEnvironment): string | undefined {
  if (props.sourceProjectId) {
    return props.sourceProjectId
  }

  const projectFile = getProjectFilePath(props, env)
  return readProjectIdFromProjectFile(env.resolver, projectFile)
}


