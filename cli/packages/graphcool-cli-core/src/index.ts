import Deploy from './commands/deploy'
import Init from './commands/init'
import Auth from './commands/auth/index'
import Info from './commands/info/index'
import Playground from './commands/playground/index'
import Console from './commands/console'
import List from './commands/list/index'
import Delete from './commands/delete/index'
import RootTokens from './commands/root-tokens/index'
import FunctionLogs from './commands/logs/function'
import Diff from './commands/diff/index'
import Pull from './commands/pull/index'
import Export from './commands/export/index'
import InvokeLocal from './commands/invoke/local'
import PullLocal from './commands/local/pull'
import Start from './commands/local/start'
import Stop from './commands/local/stop'
import Up from './commands/local/up'
import Restart from './commands/local/restart'
import Account from './commands/account'
import Eject from './commands/local/eject'

export const topics = [
  { name: 'deploy', description: 'Deploy local project definition' },
  { name: 'init', description: 'Create a new project' },
  { name: 'auth', description: 'Create account or login' },
  {
    name: 'info',
    description: 'Print project info (environments, endpoints, ...) ',
  },
  { name: 'console', description: 'Opens the console for the current project' },
  {
    name: 'playground',
    description: 'Opens the playground for the current project',
  },
  { name: 'list', description: 'List all deployed services' },
  // { name: 'modules', description: 'Manage modules' },
  { name: 'get-root-token', description: 'Get the project root tokens' },
  { name: 'functions', description: 'List all functions of a project' },
  { name: 'logs', description: 'Get logs of functions' },
  {
    name: 'diff',
    description: 'Get the diff of the local and remote project definition',
  },
  {
    name: 'delete',
    description: 'Delete a project',
  },
  {
    name: 'export',
    description: 'Export project data',
  },
  {
    name: 'invoke',
    description: 'Invokes a function locally',
  },
  {
    name: 'account',
    description: 'Information about the current authenticated account'
  }
  // {
  //   name: 'local',
  //   description: 'Manage the local Graphcool version'
  // }
]

export const commands = [
  Deploy,
  Init,
  Auth,
  Info,
  Playground,
  Console,
  List,
  Delete,
  RootTokens,
  FunctionLogs,
  Diff,
  Pull,
  Export,
  InvokeLocal,
  PullLocal,
  Start,
  Stop,
  Up,
  Restart,
  Account,
  Eject,
]
