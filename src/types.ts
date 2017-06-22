import {Config} from './utils/config'
export type Command =
  'auth'
  | 'push'
  | 'status'
  | 'init'
  | 'export'
  | 'pull'
  | 'endpoints'
  | 'console'
  | 'playground'
  | 'projects'
  | 'version'
  | 'help'
  | 'create' // TODO remove at version 1.1
  | 'quickstart'
  | 'delete'

export type Region = 'eu-west-1'

export interface GraphcoolConfig {
  token?: string
}
export type GraphcoolConfigOptionName = 'token'

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
  region: string
}

export interface MigrationMessage {
  type: string
  action: string
  name: string
  description: string
  subDescriptions?: [MigrationMessage] // only ever goes one level deep`
}

export type MigrationActionType = 'create' | 'delete' | 'update' | 'unknown'

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
  config: Config
}

export interface APIError {
  message: string
  requestId: string
  code: string
}

export type AuthTrigger = 'auth' | 'init' | 'quickstart'
export type CheckAuth = (env: SystemEnvironment, authTrigger: AuthTrigger) => Promise<boolean>
