import Deploy from './commands/deploy'
import Init from './commands/init'
import Auth from './commands/auth/index'
import RemoveEnv from './commands/env/remove'
import DefaultEnv from './commands/env/default'
import RenameEnv from './commands/env/rename'
import SetEnv from './commands/env/set'
import Info from './commands/info/index'
import Playground from './commands/playground/index'
import Console from './commands/console'
import Projects from './commands/projects/index'
import ModuleAdd from './commands/module/add'
import Delete from './commands/delete/index'
import RootTokens from './commands/root-tokens/index'
import FunctionsOverview from './commands/functions/index'
import FunctionLogs from './commands/logs/function'
import Diff from './commands/diff/index'
import Pull from './commands/pull/index'
import Export from './commands/export/index'
import InvokeLocal from './commands/invoke/local'
import PullLocal from './commands/local/pull'
import Reset from './commands/local/reset'
import Start from './commands/local/start'
import Stop from './commands/local/stop'
import Up from './commands/local/up'
import Restart from './commands/local/restart'

export const topics = [
  { name: 'deploy', description: 'Deploy local project definition' },
  { name: 'init', description: 'Create a new project' },
  { name: 'auth', description: 'Create account or login' },
  { name: 'env', description: 'Manage project environment' },
  {
    name: 'info',
    description: 'Print project info (environments, endpoints, ...) ',
  },
  { name: 'console', description: 'Opens the console for the current project' },
  {
    name: 'playground',
    description: 'Opens the playground for the current project',
  },
  { name: 'projects', description: 'List all projects' },
  { name: 'modules', description: 'Manage modules' },
  { name: 'get-root-token', description: 'Get the project root tokens' },
  { name: 'functions', description: 'List all functions of a project' },
  { name: 'logs', description: 'Get logs of functions' },
  {
    name: 'diff',
    description: 'Get the diff of the local and remote project definition',
  },
  {
    name: 'export',
    description: 'Export project data',
  },
  {
    name: 'invoke',
    description: 'Invokes a function locally',
  },
  // {
  //   name: 'local',
  //   description: 'Manage the local Graphcool version'
  // }
]

export const commands = [
  Deploy,
  Init,
  Auth,
  SetEnv,
  RemoveEnv,
  DefaultEnv,
  RenameEnv,
  Info,
  Playground,
  Console,
  Projects,
  ModuleAdd,
  Delete,
  RootTokens,
  FunctionsOverview,
  FunctionLogs,
  Diff,
  Pull,
  Export,
  InvokeLocal,
  PullLocal,
  Reset,
  Start,
  Stop,
  Up,
  Restart,
]
