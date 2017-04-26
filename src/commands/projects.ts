import {Resolver} from '../types'
import {fetchProjects} from '../api/api'
const debug = require('debug')('graphcool')
import figures = require('figures')
import {couldNotFetchProjectsMessage} from '../utils/constants'

interface Props {

}

export default async(props: Props, resolver: Resolver): Promise<void> => {

  try {
    const projects = await fetchProjects(resolver)

    const outputString = projects
      .map(project => `${figures.star}  ${project.name} (${project.projectId})`)
      .join('\n')

    process.stdout.write(outputString)
  } catch (e) {
    process.stdout.write(`${couldNotFetchProjectsMessage} ${e.message}`)
  }

}