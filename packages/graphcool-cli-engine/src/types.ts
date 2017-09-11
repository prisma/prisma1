export interface RunOptions {
  argv?: string[]
  mock: boolean
  initPath?: string
  root?: string
}

export type Region = 'EU_WEST_1' | 'AP_NORTHEAST_1' | 'US_WEST_2'

export interface AuthServer {
  requestAuthToken(): Promise<string>
  validateAuthToken(token: string)
}

export interface SchemaInfo {
  schema: string
  source: string
}

export interface Project {
  id: string
  name: string
  schema: string
  alias: string
  region: string
}

export interface RemoteProject extends Project {
  projectDefinitionWithFileContent: string
}

export interface FunctionLog {
  id: string
  requestId: string
  duration: number
  status: string
  timestamp: string
  message: string
}

export interface FunctionInfo {
  name: string
  id: string
  type: 'AUTH0' | 'WEBHOOK'
  stats: {
    requestCount: number
    errorCount: number
  }
  __typename: 'SchemaExtensionFunction' | ''
}

export interface PAT {
  id: string
  name: string
  token: string
}

export interface ProjectInfo extends Project {
  id: string
  name: string
  schema: string
  alias: string
  region: string
  projectDefinition: ProjectDefinition
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

export interface MigrateProjectPayload {
  migrationMessages: MigrationMessage[]
  errors: MigrationErrorMessage[]
  project: Project
}

export interface MigrationResult {
  migrationMessages: MigrationMessage[]
  errors: MigrationErrorMessage[]
  newSchema: string
  projectDefinition: ProjectDefinition
}

export interface APIError {
  message: string
  requestId: string
  code: string
}

export type AuthTrigger = 'auth' | 'init' | 'quickstart'
export type CheckAuth = (authTrigger: AuthTrigger) => Promise<boolean>

export interface ProjectDefinition {
  modules: GraphcoolModule[]
}

export interface GraphcoolModule {
  name: string
  content: string
  files: {[fileName: string]: string}
}

export interface EnvironmentConfig {
  default: string | null
  environments: Environments
}

export interface Environments {[environment: string]: string}
