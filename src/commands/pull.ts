import {Resolver, SystemEnvironment} from '../types'
import {readProjectIdFromProjectFile, writeProjectFile} from '../utils/file'
import {pullProjectInfo} from '../api/api'
import {fetchingProjectDataMessage, noProjectIdMessage, wroteProjectFileMessage} from '../utils/constants'
const debug = require('debug')('graphcool')
import figures = require('figures')

interface Props {
  projectId?: string
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  const projectId = props.projectId ? props.projectId : readProjectIdFromProjectFile(resolver)

  if (!projectId) {
    out.write(noProjectIdMessage)
    process.exit(0)
  }

  out.startSpinner(fetchingProjectDataMessage)
  const projectInfo = await pullProjectInfo(projectId!, resolver)

  debug(`Project Info: \n\n${JSON.stringify(projectInfo)}`)

  out.stopSpinner()
  writeProjectFile(projectInfo, resolver)

  out.write(wroteProjectFileMessage)

}