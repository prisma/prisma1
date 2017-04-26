import {Resolver, SystemEnvironment} from '../types'
import {fetchProjects} from '../api/api'
const debug = require('debug')('graphcool')
import figures = require('figures')
import {couldNotFetchProjectsMessage} from '../utils/constants'

interface Props {

}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  try {
    const projects = await fetchProjects(resolver)

    const outputString = projects
      .map(project => `${figures.star}  ${project.name} (${project.projectId})`)
      .join('\n')

    out.write(outputString)
  } catch (e) {
    out.write(`${couldNotFetchProjectsMessage} ${e.message}`)
  }

}