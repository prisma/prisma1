import {GraphcoolConfig, Resolver, ProjectInfo} from '../types'
import {graphcoolConfigFilePath, graphcoolProjectFileName, projectFileSuffixes, exampleSchema} from './constants'
import * as path from 'path'
const debug = require('debug')('graphcool')
import * as fs from 'fs'

/*
 * Project File (.../project.graphcool)
 */

export function writeProjectFile(projectInfo: ProjectInfo, resolver: Resolver, path?: string) {
  path = isValidProjectFilePath(path) ? path : graphcoolProjectFileName
  const schemaWithHeader = `# project: ${projectInfo.projectId}\n# version: ${projectInfo.version || ''}\n\n${projectInfo.schema}`
  resolver.write(path!, schemaWithHeader)
}

export function readProjectInfoFromProjectFile(resolver: Resolver, path?: string): ProjectInfo | undefined {
  const projectId = readProjectIdFromProjectFile(resolver, path)

  if (!projectId) {
    return undefined
  }

  const version = readVersionFromProjectFile(resolver, path)
  const schema = readDataModelFromProjectFile(resolver, path)

  debug(`Read project info: ${projectId}, ${version}`)

  return { projectId, version, schema} as ProjectInfo
}

export function readProjectIdFromProjectFile(resolver: Resolver, path?: string): string | undefined {
  const pathToProjectFile = getPathToProjectFile(path)
  const contents = resolver.read(pathToProjectFile)

  const matches = contents.match(/# project: ([a-z0-9]*)/)

  if (!matches || matches.length !== 2) {
    return undefined
    // throw new Error(`${pathToProjectFile} doesn't contain a project ID.`)
  }

  return matches[1]
}

export function readVersionFromProjectFile(resolver: Resolver, path?: string): string | undefined {
  const pathToProjectFile = getPathToProjectFile(path)
  const contents = resolver.read(pathToProjectFile)

  const matches = contents.match(/# version: ([a-z0-9]*)/)

  if (!matches || matches.length !== 2) {
    return undefined
  }

  return matches[1]
}

export function readDataModelFromProjectFile(resolver: Resolver, path?: string): string {
  const pathToProjectFile =getPathToProjectFile(path)
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

  // the path only points to a directory
  return path.join(filePath, graphcoolProjectFileName)
}

function isValidProjectFilePath(projectFilePath?: string): boolean {
  if (!projectFilePath) {
    return false
  }
  return projectFilePath.endsWith('.graphcool')
}

export function writeExampleSchemaFile(resolver: Resolver): string {
  const path = 'example.graphql'
  resolver.write(path, exampleSchema)
  return path
}


/*
 * Graphcool Config (~/.graphcool)
 */

export function readGraphcoolConfig(resolver: Resolver): GraphcoolConfig {
  const configFileContent = resolver.read(graphcoolConfigFilePath)
  return JSON.parse(configFileContent)
}

export function writeGraphcoolConfig(config: GraphcoolConfig, resolver: Resolver): void {
  resolver.write(graphcoolConfigFilePath, JSON.stringify(config, null, 2))
}

export function deleteGraphcoolConfig(resolver: Resolver): void {
  resolver.delete(graphcoolConfigFilePath)
}
