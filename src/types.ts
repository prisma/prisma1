export type Command = 'auth' | 'push' | 'init' | 'eject' | 'export' | 'pull' | 'import' | 'projects' | 'update' | 'version' | 'help' | 'create'

export type Region = 'eu-west-1'

export interface GraphcoolConfig {
  token: string
}

export interface Resolver {
  read(path: string): string
  write(path: string, value: string)
  delete(path: string)
  exists(path: string): boolean
}

export type TokenValidationResult = 'valid' | 'invalid'

export interface AuthServer {
  requestAuthToken(): Promise<string>
  validateAuthToken(token: string): Promise<TokenValidationResult>
}

export interface SchemaInfo {
  schema: string
  source: string
}

export interface ProjectInfo {
  projectId: string
  name?: string
  schema?: string
  version?: string
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
}

export interface Out {
  write(message: string): void
  startSpinner(message: string): void
  stopSpinner(): void
}

export interface SystemEnvironment {
  out: Out
  resolver: Resolver
}

export interface APIError {
  message: string
  requestId: string
  code: string
}