import { Region } from '../types'
import 'isomorphic-fetch'
// import cloneCommand from './clone'
import { projectInfoToContents } from '../utils/utils'
import { isValidProjectName } from '../utils/validation'
import {
  couldNotCreateProjectMessage,
  createdProjectMessage,
  creatingProjectMessage,
  defaultDefinition,
  envExistsButNoEnvNameProvided,
  invalidProjectNameMessage,
} from '../utils/constants'
import out from '../io/Out'
import env from '../io/Environment'
import client from '../io/Client'
import { generateErrorOutput, parseErrors } from '../utils/errors'
import definition from '../io/ProjectDefinition/ProjectDefinition'
import generateName = require('sillyname')

export interface InitProps {
  name?: string
  alias?: string
  region?: Region
  outputPath?: string
  env?: string
  blank?: boolean
}

// TODO: Blank project detection: We don't have a single file anymore, but projects now.

export default async (props: InitProps): Promise<void> => {
  // create new
  if (env.default && !props.env) {
    throw new Error(envExistsButNoEnvNameProvided(env.env))
  }

  if (props.name && !isValidProjectName(props.name)) {
    throw new Error(invalidProjectNameMessage(props.name))
  }

  const name = props.name || generateName()
  out.startSpinner(creatingProjectMessage(name))

  // TODO refactor completely. We're not creating a project based on a schema anymore, but the complete project definition
  try {
    // TODO later add default / empty definition if there isn't a definition in this folder yet
    const projectDefinition = props.blank ? definition.definition : defaultDefinition

    // create project
    const project = await client.createProject(name, projectDefinition, props.alias, props.region)

    // add environment
    await env.set(props.env || 'dev', project.id)
    env.save()

    out.stopSpinner()

    const message = createdProjectMessage(name, project.id, projectInfoToContents(project), props.outputPath)
    out.write(message)

  } catch (e) {
    out.stopSpinner()
    out.writeError(`${couldNotCreateProjectMessage}`)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }
}
