export type Command = 'auth' | 'create' | 'eject' | 'export' | 'fetch' | 'import' | 'projects' | 'update' | 'version' | 'help'

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
  getAuthToken(): Promise<string> // returns token
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
