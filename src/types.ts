import {Config} from './utils/config'
import { AuthProps } from './commands/auth'
import { InteractiveInitProps } from './commands/interactiveInit'
import { PushProps } from './commands/push'
import { PlaygroundProps } from './commands/playground'
import { InitProps } from './commands/init'
import { ExportProps } from './commands/export'
import { DeleteProps } from './commands/delete'
import { ConsoleProps } from './commands/console'
import { CloneProps } from './commands/clone'
import { EndpointsProps } from './commands/endpoints'
import { ProjectsProps } from './commands/projects'
import { PullProps } from './commands/pull'
import { StatusProps } from './commands/status'
import { QuickstartProps } from './commands/quickstart'
export type Command =
  'auth'
  | 'push'
  | 'status'
  | 'init'
  | 'interactiveInit'
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
  | 'usage'
  | 'delete'
  | 'unknown'

export type CommandProps =
  AuthProps
  | CloneProps
  | ConsoleProps
  | DeleteProps
  | EndpointsProps
  | ExportProps
  | InitProps
  | InteractiveInitProps
  | PlaygroundProps
  | ProjectsProps
  | PullProps
  | PushProps
  | QuickstartProps
  | StatusProps
  | UsageProps

interface UsageProps {
  command: Command
}

export interface CommandInstruction {
  props?: CommandProps
  command?: Command
}

// TODO Remove in favor of ProjectRegion
export type Region = 'eu_west_1' | 'ap_northeast_1' | 'us_west_2'

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
// TODO remove after cleanup
//
// export interface Out {
//   write(message: string): void
//   writeError(message: string): void
//   startSpinner(message: string): void
//   stopSpinner(): void
//   onError(error: Error): void
// }
//
// export interface SystemEnvironment {
//   out: Out
//   config: Config
// }

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

export interface ProjectEnvironment {
  version: number
  projectType: ProjectType
  hosted: ProjectEnvironmentHosted
}

type ProjectType = 'HOSTED' | 'DOCKER'
type ProjectRegion = 'EU_WEST_1' | 'AP_NORTHEAST_1' | 'US_WEST_2'

export interface ProjectEnvironmentHosted {
  project: string // can be project id or alias
  region: ProjectRegion
}
