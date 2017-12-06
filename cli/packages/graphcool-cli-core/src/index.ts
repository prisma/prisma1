import Deploy from './commands/deploy'
// import Init from './commands/init'
// import Auth from './commands/auth/index'
// import Info from './commands/info/index'
// import Playground from './commands/playground/index'
// import Console from './commands/console'
// import List from './commands/list/index'
// import Delete from './commands/delete/index'
// import RootTokens from './commands/root-token/index'
// import FunctionLogs from './commands/logs/function'
// import PullLocal from './commands/local/pull'
// import Stop from './commands/local/stop'
// import Up from './commands/local/up'
// import Restart from './commands/local/restart'
// import Account from './commands/account'
// import Eject from './commands/local/eject'
// import PsLocal from './commands/local/ps'
// import Reset from './commands/reset/reset'
// import Clusters from './commands/clusters/index'

export const groups = [
  {
    key: 'general',
    name: 'General commands',
  },
  // {
  //   key: 'data',
  //   name: 'Data workflows',
  // },
  // {
  //   key: 'local',
  //   name: 'Local development',
  // },
  // {
  //   key: 'platform',
  //   name: 'Platform',
  // },
]

export const topics = [
  { name: 'init', description: 'Create a new service', group: 'general' },
  {
    name: 'deploy',
    description: 'Deploy local service definition',
    group: 'general',
  },
  // { name: 'login', description: 'Create account or login', group: 'platform' },
  // { name: 'reset', description: 'Reset data of a service', group: 'data' },
  // {
  //   name: 'playground',
  //   description: 'Opens the playground for the current service',
  //   group: 'general',
  // },
  // {
  //   name: 'info',
  //   description: 'Print service info (endpoints, clusters, ...) ',
  //   group: 'general',
  // },
  // { name: 'list', description: 'List all deployed services', group: 'general' },
  // {
  //   name: 'root-token',
  //   description: 'Get the service root tokens',
  //   group: 'general',
  // },
  // { name: 'logs', description: 'Get logs of functions', group: 'general' },
  // {
  //   name: 'delete',
  //   description: 'Delete a service',
  //   group: 'general',
  // },
  // {
  //   name: 'console',
  //   description: 'Opens the console for the current service',
  //   group: 'platform',
  // },
  // {
  //   name: 'account',
  //   description: 'Information about the current authenticated account',
  //   group: 'platform',
  // },
  // {
  //   name: 'local',
  //   description: 'Manage the local Graphcool version',
  //   group: 'local',
  // },
  // {
  //   name: 'clusters',
  //   description: 'Manage your private clusters',
  //   group: 'general',
  // },
]

export const commands = [
  Deploy,
  // Init,
  // Auth,
  // Info,
  // Playground,
  // Console,
  // List,
  // Delete,
  // RootTokens,
  // FunctionLogs,
  // PullLocal,
  // Stop,
  // Up,
  // Restart,
  // Account,
  // Eject,
  // PsLocal,
  // Reset,
  // Clusters,
]

export {
  Deploy,
  // Init,
  // Auth,
  // Info,
  // Playground,
  // Console,
  // List,
  // Delete,
  // RootTokens,
  // FunctionLogs,
  // PullLocal,
  // Stop,
  // Up,
  // Restart,
  // Account,
  // Eject,
  // PsLocal,
  // Reset,
  // Clusters,
}
