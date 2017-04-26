import {AuthConfig, Resolver, ProjectInfo} from '../types'
import {authConfigFilePath, graphcoolProjectFileName, projectFileSuffixes} from './constants'
import * as path from 'path'
const debug = require('debug')('graphcool')

export function writeProjectFile(projectInfo: ProjectInfo, resolver: Resolver) {
  const schemaWithHeader = `# @project ${projectInfo.projectId}\n# @version ${projectInfo.version}\n\n${projectInfo.schema}`
  resolver.write(graphcoolProjectFileName, schemaWithHeader)
}

export function readProjectIdFromProjectFile(resolver: Resolver, path?: string): string {
  const pathToProjectFile = getPathToProjectFile(path)
  debug(`Read project ID from file: ${pathToProjectFile}`)
  const contents = resolver.read(pathToProjectFile)

  const matches = contents.match(/# @project ([a-z0-9]*)/)

  if (!matches || matches.length !== 2) {
    throw new Error(`${graphcoolProjectFileName} doesn't contain a project ID.`)
  }

  return matches[1]
}

export function readDataModelFromProjectFile(resolver: Resolver, path?: string): string {
  const pathToProjectFile = path ? `${path}/${graphcoolProjectFileName}` : `./${graphcoolProjectFileName}`
  const contents = resolver.read(pathToProjectFile)

  const dataModelStartIndex = contents.indexOf(`type`)
  const dataModel = contents.substring(dataModelStartIndex, contents.length)
  return dataModel
}

function getPathToProjectFile(filePath?: string): string {
  if (!filePath) {
    return `./${graphcoolProjectFileName}` // default filePath
  }

  // does the path already point to a project file
  const projectFileResult = projectFileSuffixes.filter(suffix => filePath.endsWith(suffix))
  if (projectFileResult.length > 0) {
    return filePath
  }

  // TODO: search in current directory for file with suffix

  // the path only points to a directory
  return path.join(filePath, graphcoolProjectFileName)
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
