import {AuthConfig, Resolver, ProjectInfo} from '../types'
import { authConfigFilePath, graphcoolProjectFileName } from './constants'

export function writeProjectFile(projectInfo: ProjectInfo, resolver: Resolver) {
  const schemaWithHeader = `# @project ${projectInfo.projectId}\n# @version ${projectInfo.version}\n\n${projectInfo.schema}`
  resolver.write(graphcoolProjectFileName, schemaWithHeader)
}

export function readProjectIdFromProjectFile(resolver: Resolver): string {
  const contents = resolver.read(graphcoolProjectFileName)

  const matches = contents.match(/# @project ([a-z0-9]*)/)

  if (!matches || matches.length !== 2) {
    throw new Error(`${graphcoolProjectFileName} doesn't contain a project ID.`)
  }

  return matches[1]
}

export function readDataModelFromProjectFile(resolver: Resolver): string {
  const contents = resolver.read(graphcoolProjectFileName)

  const dataModelStartIndex = contents.indexOf(`@type`)
  const dataModel = contents.substring(dataModelStartIndex, contents.length)
  return dataModel
}

export function readAuthConfig(resolver: Resolver): AuthConfig {
  const configFileContent = resolver.read(authConfigFilePath)
  return { token: configFileContent }
}

export function writeAuthConfig(config: AuthConfig, resolver: Resolver): void {
  resolver.write(authConfigFilePath, JSON.stringify(config, null, 2))
}

export function deleteAuthConfig(resolver: Resolver): void {
  resolver.delete(authConfigFilePath)
}
