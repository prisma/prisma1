import * as fs from 'fs'
import * as os from 'os'
import * as path from 'path'
import {AuthConfig, Resolver} from '../types'

// export function writeProjectFile(schema: string, projectId: string) {
//   const schemaWithHeader = `# projectId: "${projectId}"\n\n${schema}`
//   fs.writeFileSync('project.graphcool', schemaWithHeader)
// }

// export function readProjectIdFromSchemaFile(): string {
//   const contents = fs.readFileSync('graphcool.schema').toString()
//
//   const matches = contents.match(/# projectId: "([a-z0-9]*)"/)
//
//   if (!matches || matches.length !== 2) {
//     throw new Error(`graphcool.schema doesn't contain a projectId`)
//   }
//
//   return matches[1]
// }

const configFilePath = path.join(os.homedir(), '.graphcool')


export class FileSystemResolver implements Resolver {
  read(path: string): string {
    return fs.readFileSync(configFilePath).toString()
  }

  write(path: string, value: string) {
    fs.writeFileSync(path, value)
  }

  delete(path: string) {
    fs.unlinkSync(path)
  }
}

export class TestResolver implements Resolver {

  storage: { [key: string] : string }

  constructor(storage: { [key: string] : string }) {
    this.storage = storage
  }

  read(path: string): string {
    return this.storage[path]
  }

  write(path: string, value: string) {
    this.storage[path] = value
  }

  delete(path: string) {
    delete this.storage[path]
  }

}

export function readAuthConfig(resolver: Resolver): AuthConfig {
  const configFileContent = resolver.read(configFilePath)
  return { token: configFileContent }
}

export function writeAuthConfig(config: AuthConfig, resolver: Resolver): void {
  resolver.write(configFilePath, JSON.stringify(config, null, 2))
}

export function deleteAuthConfig(resolver: Resolver): void {
  resolver.delete(configFilePath)
}
