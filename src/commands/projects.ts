import { SystemEnvironment } from '../types'
import { fetchProjects } from '../api/api'
import figures = require('figures')
import {couldNotFetchProjectsMessage, graphcoolProjectFileName} from '../utils/constants'
import { readProjectIdFromProjectFile } from '../utils/file'
import { regionEnumToOption } from '../utils/utils'

const {table, getBorderCharacters} = require('table')
const debug = require('debug')('graphcool')

export interface ProjectsProps {

}

export default async (props: ProjectsProps, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  try {
    const projects = await fetchProjects(resolver)

    const currentProjectId = resolver.exists(graphcoolProjectFileName) ?
      readProjectIdFromProjectFile(resolver, graphcoolProjectFileName) :
      null

    const data = projects.map(project => {
      const isCurrentProject = currentProjectId !== null && (currentProjectId === project.projectId || currentProjectId === project.alias)
      return [isCurrentProject ? '*' : ' ', `${project.alias || project.projectId}   `, project.name, regionEnumToOption(project.region)]
    })

    const output = table(data, {
      border: getBorderCharacters('void'),
      columnDefault: {
        paddingLeft: '0',
        paddingRight: '2',
      },
      drawHorizontalLine: () => false,
    }).trimRight()

    out.write(output)
  } catch (e) {
    throw new Error(`${couldNotFetchProjectsMessage} ${e.message}`)
  }

}
