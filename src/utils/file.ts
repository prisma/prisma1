import { GraphcoolConfig, ProjectInfo, ProjectEnvironment } from '../types'
import gclToJson from 'gcl-lib'
import * as fs from 'fs'
import * as path from 'path'
// import { projectInfoToContents } from './utils'
// import {
//   graphcoolConfigFilePath,
//   // graphcoolProjectFileName,
//   exampleSchema,
//   projectFileSuffix, schemaFileSuffix, blankProjectFileFromExampleSchema
// } from './constants'

// const debug = require('debug')('graphcool')

export async function readProjectEnvironment(envPath: string): Promise<ProjectEnvironment> {
  const schema = fs.readFileSync(path.join(__dirname, '../../../src/gcl/env-schema.graphql'), 'utf-8')
  const envString = fs.readFileSync(envPath, 'utf-8')

  return await gclToJson(envString, schema) as ProjectEnvironment
}

/*
 * Project File (.../project.graphcool)
 */

// export function writeProjectFile(projectInfo: ProjectInfo, resolver: Resolver, path?: string) {
//   path = isValidProjectFilePath(path) ? path : graphcoolProjectFileName
//   resolver.write(path!, projectInfoToContents(projectInfo))
// }
//
// export function writeBlankProjectFileWithInfo(projectInfo: ProjectInfo, resolver: Resolver, path?: string) {
//   path = isValidProjectFilePath(path) ? path : graphcoolProjectFileName
//   const fullProjectFile = blankProjectFileFromExampleSchema(
//     projectInfo.alias ? projectInfo.alias : projectInfo.projectId, projectInfo.version)
//   resolver.write(path!, fullProjectFile)
// }


// export function readProjectInfoFromProjectFile(resolver: Resolver, path: string): ProjectInfo | undefined {
//   const projectId = readProjectIdFromProjectFile(resolver, path)
//
//   if (!projectId) {
//     return undefined
//   }
//
//   const version = readVersionFromProjectFile(resolver, path)
//   const schema = readDataModelFromProjectFile(resolver, path)
//
//   return { projectId, version, schema} as ProjectInfo
// }
//
// export function readProjectIdFromProjectFile(resolver: Resolver, path: string): string | undefined {
//   const contents = resolver.read(path)
//   const matches = contents.match(/# project: ([a-zA-Z0-9-]*)/)
//
//   if (!matches || matches.length !== 2) {
//     return undefined
//   }
//
//   return matches[1]
// }
//
// export function readVersionFromProjectFile(resolver: Resolver, path: string): string | undefined {
//   const contents = resolver.read(path)
//   const matches = contents.match(/# version: ([a-z0-9-]*)/)
//
//   if (!matches || matches.length !== 2) {
//     return undefined
//   }
//
//   return matches[1]
// }
//
// export function readDataModelFromProjectFile(resolver: Resolver, path: string): string {
//   const contents = resolver.read(path)
//
//   const dataModelStartIndex = contents.indexOf(`type`)
//   const dataModel = contents.substring(dataModelStartIndex, contents.length)
//   return dataModel
// }
//
//
// export function isValidProjectFilePath(projectFilePath?: string): boolean {
//   if (!projectFilePath) {
//     return false
//   }
//   return projectFilePath.endsWith(projectFileSuffix)
// }
//
// export function isValidSchemaFilePath(schemaFilePath?: string): boolean {
//   if (!schemaFilePath) {
//     return false
//   }
//   return schemaFilePath.endsWith(schemaFileSuffix)
// }
