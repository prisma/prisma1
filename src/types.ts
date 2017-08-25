import { AuthProps } from './commands/auth'
// import { InteractiveInitProps } from './commands/interactiveInit'
// import { PushProps } from './commands/push'
import { PlaygroundProps } from './commands/playground'
import { InitProps } from './commands/init'
import { ExportCliProps } from './commands/export'
import { DeleteCliProps } from './commands/delete'
import { ConsoleProps } from './commands/console'
import { CloneProps } from './commands/clone'
import { ProjectsProps } from './commands/projects'
import { PullCliProps } from './commands/pull'
import { QuickstartProps } from './commands/quickstart'
import { InfoCliProps } from './commands/info'

export type Command =
  'auth'
  | 'push'
  | 'init'
  | 'interactiveInit'
  | 'export'
  | 'pull'
  | 'info'
  | 'console'
  | 'playground'
  | 'projects'
  | 'version'
  | 'help'
  | 'create' // TODO remove at version 1.1
  | 'quickstart'
  | 'usage'
  | 'delete'
  | 'unknown'

export type CommandProps =
  AuthProps
  | CloneProps
  | ConsoleProps
  | DeleteCliProps
  | ExportCliProps
  | InitProps
  | InfoCliProps
  | PlaygroundProps
  | ProjectsProps
  | PullCliProps
  | QuickstartProps
  | UsageProps
// | InteractiveInitProps
// | PushProps

interface UsageProps {
  command: Command
}

export interface CommandInstruction {
  props?: CommandProps
  command?: Command
}

// TODO Remove in favor of ProjectRegion
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
  version: string
  alias: string
  region: string
}

export interface RemoteProject extends Project {
  projectDefinitionWithFileContent: string
}

export interface ProjectInfo extends Project {
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
  newVersion: string
  newSchema: string
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

export type ProjectEnvironment = HostedProjectEnvironment | DockerProjectEnvironment

export interface Environments {[environment: string]: ProjectEnvironment}

export interface HostedProjectEnvironment {
  projectId: string
  version: number
}

export interface DockerProjectEnvironment {
  projectId: string
  port: number
  version: number
}
