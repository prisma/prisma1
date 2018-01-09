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

export const groups = [
  {
    key: 'db',
    name: 'Database service',
  },
  {
    key: 'data',
    name: 'Data workflows',
  },
  {
    key: 'cloud',
    name: 'Cloud',
  },
  {
    key: 'local',
    name: 'Local development',
  },
  {
    key: 'clusters',
    name: 'Clusters',
  },
]

export const topics = [
  /* Database service */
  {
    name: 'init',
    description: 'Create files for new services',
    group: 'db',
  },
  {
    name: 'deploy',
    description: 'Deploy local service definition',
    group: 'db',
  },
  {
    name: 'info',
    description: 'Print service info (endpoints, clusters, ...) ',
    group: 'db',
  },
  { name: 'token', description: 'Create a new service token', group: 'db' },
  { name: 'list', description: 'List all deployed services', group: 'db' },
  { name: 'delete', description: 'Delete an existing service', group: 'db' },
  /* Data workflows */
  {
    name: 'playground',
    description: 'Opens the playground for the current service',
    group: 'data',
  },
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
  /* Local development */
  {
    name: 'local',
    description: 'Manage the local Graphcool version',
    group: 'local',
  },
  /* Cloud */
  {
    name: 'console',
    description: 'Opens the console for the current service',
    group: 'cloud',
  },
  {
    name: 'login',
    description: 'Login or signup to Graphcool Cloud',
    group: 'cloud',
  },
  {
    name: 'account',
    description: 'Print account information',
    group: 'cloud',
  },
  /* Clusters */
  { name: 'cluster', description: 'Manage local clusters', group: 'clusters' },
]

export const commands = [
  Deploy,
  Init,
  // Auth,
  Info,
  Playground,
  ConsoleCommand,
  List,
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
]

export {
  Deploy,
  Init,
  // Auth,
  Info,
  Playground,
  // Console,
  List,
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
}
