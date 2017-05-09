import TestOut from './system/TestOut'
export type Command =
  'auth'
  | 'push'
  | 'status'
  | 'init'
  | 'clone'
  | 'export'
  | 'pull'
  | 'endpoints'
  | 'console'
  | 'playground'
  | 'projects'
  | 'version'
  | 'help'
  | 'create'

export type Region = 'eu-west-1'

export interface GraphcoolConfig {
  token: string
}

export interface Resolver {
  read(path: string): string
  write(path: string, value: string)
  delete(path: string)
  exists(path: string): boolean
  projectFiles(directory?: string): string[]
  schemaFiles(directory?: string): string[]
  readDirectory(path: string): string[]
}

export interface AuthServer {
  requestAuthToken(): Promise<string>
  validateAuthToken(token: string)
}

export interface SchemaInfo {
  schema: string
  source: string
}

export interface ProjectInfo {
  projectId: string
  name: string
  schema: string
  version: string
  alias: string
}

export interface MigrationMessage {
  type: string
  action: string
  name: string
  description: string
  subDescriptions?: [MigrationMessage]
}

export interface MigrationErrorMessage {
  type: string
  description: string
  field: string
}

export interface MigrationResult {
  messages: [MigrationMessage]
  errors: [MigrationErrorMessage]
  newVersion: string
  newSchema: string
}

export interface Out {
  write(message: string): void
  writeError(message: string): void
  startSpinner(message: string): void
  stopSpinner(): void
  onError(error: Error): void
}

export interface SystemEnvironment {
  out: Out
  resolver: Resolver
}

export interface TestSystemEnvironment {
  out: TestOut
  resolver: Resolver
}

export interface APIError {
  message: string
  requestId: string
  code: string
}
