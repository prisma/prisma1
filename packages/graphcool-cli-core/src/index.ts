import Deploy from './commands/deploy'
import Init from './commands/init'
import Auth from './commands/auth/index'
export const topics = [
  { name: 'deploy', description: 'Deploy local project definition' },
  { name: 'init', description: 'Create a new project' },
  { name: 'auth', description: 'Create account or login' }
]

export const commands = [Deploy, Init, Auth]
