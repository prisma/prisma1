import {Resolver} from '../types'
import {readProjectIdFromProjectFile, writeProjectFile} from '../utils/file'
import {pullProjectInfo} from '../api/api'
import {fetchingProjectDataMessage, noProjectIdMessage, wroteProjectFileMessage} from '../utils/constants'
import ora = require('ora')
const debug = require('debug')('graphcool')
import figures = require('figures')

interface Props {
  projectId?: string
}

export default async(props: Props, resolver: Resolver): Promise<void> => {

  try {
    const projectId = props.projectId ? props.projectId : readProjectIdFromProjectFile(resolver)

    const spinner = ora(fetchingProjectDataMessage).start()
    const projectInfo = await pullProjectInfo(projectId, resolver)

    debug(`Project Info: \n\n${JSON.stringify(projectInfo)}`)

    spinner.stop()
    writeProjectFile(projectInfo, resolver)

    process.stdout.write(wroteProjectFileMessage)

  } catch (e) {
    process.stdout.write(noProjectIdMessage)
  }


}