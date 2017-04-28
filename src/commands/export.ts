import {SystemEnvironment} from '../types'
import {readProjectIdFromProjectFile} from '../utils/file'
import {noProjectIdMessage, exportingDataMessage, noProjectFileMessageFound} from '../utils/constants'
import {exportProjectData, parseErrors, generateErrorOutput} from '../api/api'
const debug = require('debug')('graphcool')

interface Props {
  projectId?: string
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {
    const projectId = props.projectId ? props.projectId : readProjectIdFromProjectFile(resolver)

    if (!projectId) {
      out.write(noProjectIdMessage)
      process.exit(0)
    }

    out.startSpinner(exportingDataMessage)
    const data = await exportProjectData(projectId!, resolver)

    debug(`Received data: ${data}`)

    // debug(`Project Info: \n\n${JSON.stringify(projectInfo)}`)
    //
    // out.stopSpinner()
    // writeProjectFile(projectInfo, resolver)
    //
    // out.write(wroteProjectFileMessage)

  } catch(e) {
    out.stopSpinner()
    debug(`Received error: ${JSON.stringify(e)}`)
    //
    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)

      out.write(`${output}`)
      process.exit(0)
    }

    out.write(noProjectFileMessageFound)
    process.exit(0)
  }

}