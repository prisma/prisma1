import {SystemEnvironment, Resolver} from '../types'
import {readProjectIdFromProjectFile, writeProjectFile, readVersionFromProjectFile} from '../utils/file'
import { pullProjectInfo, parseErrors, generateErrorOutput } from '../api/api'
import figures = require('figures')
import {
  fetchingProjectDataMessage,
  noProjectIdMessage,
  wroteProjectFileMessage,
  newVersionMessage, differentProjectIdWarningMessage
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

    const projectId = getProjectId(props, resolver)
    const currentVersion = getCurrentVersion(props, resolver)

    if (!projectId) {
      out.writeError(noProjectIdMessage)
      process.exit(1)
    }

    // warn if the current project file is different from specified project id
    const readProjectId = readProjectIdFromProjectFile(resolver)
    if (readProjectId && projectId !== readProjectId) {
      out.write(differentProjectIdWarningMessage(projectId, readProjectId))
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

function getProjectId(props: Props, resolver: Resolver): string {
  let projectId
  if (props.projectFile) {
    projectId = readProjectIdFromProjectFile(resolver, props.projectFile)
  } else if (props.sourceProjectId) {
    projectId = props.sourceProjectId
  } else {
    projectId = readProjectIdFromProjectFile(resolver)
  }
  return projectId
}


function getCurrentVersion(props: Props, resolver: Resolver): string {
  let currentVersion
  if (props.projectFile) {
    currentVersion = readVersionFromProjectFile(resolver, props.projectFile)
  } else {
    currentVersion = readVersionFromProjectFile(resolver)
  }
  return currentVersion
}