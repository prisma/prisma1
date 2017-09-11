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
import GetRootToken from './commands/get-root-token/index'
import FunctionsOverview from './commands/functions/index'

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
  { name: 'module', description: 'Manage modules' },
  { name: 'get-root-token', description: 'Get the project root tokens' },
  { name: 'functions', description: 'List all functions of a project' },
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
  GetRootToken,
  FunctionsOverview,
]
