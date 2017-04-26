export type Command = 'auth' | 'push' | 'create' | 'eject' | 'export' | 'fetch' | 'import' | 'projects' | 'update' | 'version' | 'help'

export type Region = 'eu-west-1'

export interface AuthConfig {
  token: string
}

export interface Resolver {
  read(path: string): string
  write(path: string, value: string)
  delete(path: string)
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
  version: string
  schema: string
}

export interface MigrationMessage {
  type: string
  action: string
  name: string
  description: string
  subDescriptions?: [MigrationMessage]
}
