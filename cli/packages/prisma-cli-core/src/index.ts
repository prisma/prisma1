import Deploy from './commands/deploy'
import Init from './commands/init'
// import Auth from './commands/auth/index'
import Info from './commands/info/index'
import Playground from './commands/playground/index'
// import Console from './commands/console'
import List from './commands/list/index'
// import Delete from './commands/delete/index'
// import RootTokens from './commands/root-token/index'
// import FunctionLogs from './commands/logs/function'
import UpgradeLocal from './commands/local/upgrade'
import Stop from './commands/local/stop'
import Up from './commands/local/up'
import Eject from './commands/local/eject'
// import PsLocal from './commands/local/ps'
import Account from './commands/account/account'
import Reset from './commands/reset/reset'
import ClusterList from './commands/cluster/list'
import Import from './commands/import/index'
import Export from './commands/export/index'
import Nuke from './commands/local/nuke'
import ConsoleCommand from './commands/console/index'
import Logs from './commands/local/logs'
import PsLocal from './commands/local/ps'
import Token from './commands/token/token'
import Login from './commands/login/login'
import ClusterToken from './commands/token/cluster-token'
import Delete from './commands/delete/index'
import ClusterLogs from './commands/cluster/logs'
import ClusterAdd from './commands/cluster/add'
import ClusterRemove from './commands/cluster/remove'
import InitPrisma from './commands/init-prisma'
import IntrospectCommand from './commands/introspect/introspect'
import Seed from './commands/seed/seed'

export const groups = [
  {
    key: 'service',
    name: 'Service',
  },
  {
    key: 'data',
    name: 'Data workflows',
  },
  {
    key: 'cloud',
    name: 'Cloud',
  },
]

export const topics = [
  /* Database service */
  {
    name: 'init',
    description: 'Create files for new services',
    group: 'service',
  },
  {
    name: 'deploy',
    description: 'Deploy local service definition',
    group: 'service',
  },
  {
    name: 'introspect',
    description: 'Introspect database schema(s) of service',
    group: 'service',
  },
  {
    name: 'info',
    description: 'Print service info (endpoints, clusters, ...) ',
    group: 'service',
  },
  {
    name: 'token',
    description: 'Create a new service token',
    group: 'service',
  },
  { name: 'list', description: 'List all deployed services', group: 'service' },
  {
    name: 'delete',
    description: 'Delete an existing service',
    group: 'service',
  },
  /* Data workflows */
  {
    name: 'playground',
    description: 'Opens the playground for the current service',
    group: 'data',
  },
  { name: 'seed', description: 'Load seed data', group: 'data' },
  {
    name: 'import',
    description: 'Import command',
    group: 'data',
  },
  {
    name: 'export',
    description: 'Export command',
    group: 'data',
  },
  { name: 'reset', description: 'Reset data of a service', group: 'data' },
  /* Cloud */
  {
    name: 'login',
    description: 'Login or signup to Prisma Cloud',
    group: 'cloud',
  },
  {
    name: 'console',
    description: 'Opens the console for the current service',
    group: 'cloud',
  },
  {
    name: 'account',
    description: 'Print account information',
    group: 'cloud',
  },
  /* Clusters */
  { name: 'cluster', description: 'Manage local clusters', group: 'cluster' },
  { name: 'init-prisma', description: 'Manage local clusters' },
]

export const commands = [
  Deploy,
  Init,
  // Auth,
  Info,
  Playground,
  ConsoleCommand,
  List,
  Seed,
  Delete,
  Up,
  Stop,
  Logs,
  UpgradeLocal,
  Eject,
  Nuke,
  Reset,
  Import,
  Export,
  PsLocal,
  Token,
  Login,
  Account,
  ClusterToken,
  ClusterList,
  ClusterLogs,
  ClusterAdd,
  ClusterRemove,
  IntrospectCommand,
]

export {
  Deploy,
  Init,
  // Auth,
  Info,
  Playground,
  // Console,
  List,
  Seed,
  Delete,
  // RootTokens,
  // FunctionLogs,
  UpgradeLocal,
  Stop,
  Up,
  Eject,
  Logs,
  PsLocal,
  Reset,
  Account,
  Import,
  Export,
  Token,
  Login,
  ClusterToken,
  ClusterList,
  ClusterLogs,
  ClusterAdd,
  ClusterRemove,
  InitPrisma,
  IntrospectCommand,
}
