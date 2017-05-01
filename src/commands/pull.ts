import { SystemEnvironment } from '../types'
import {readProjectIdFromProjectFile, writeProjectFile, readVersionFromProjectFile} from '../utils/file'
import { pullProjectInfo, parseErrors, generateErrorOutput } from '../api/api'
import figures = require('figures')
import {
  fetchingProjectDataMessage, noProjectIdMessage, wroteProjectFileMessage,
  noProjectFileMessageFound, newVersionMessage
} from '../utils/constants'

const debug = require('debug')('graphcool')

interface Props {
  sourceProjectId?: string
  projectFile?: string
  outputPath?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  try {

    let projectId, currentVersion
    if (props.projectFile) {
      projectId = readProjectIdFromProjectFile(resolver, props.projectFile)
      currentVersion = readVersionFromProjectFile(resolver, props.projectFile)
    } else if (props.sourceProjectId) {
      projectId = props.sourceProjectId
    } else {
      projectId = readProjectIdFromProjectFile(resolver)
      currentVersion = readVersionFromProjectFile(resolver)
    }

    if (!projectId) {
      out.writeError(noProjectIdMessage)
      process.exit(1)
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