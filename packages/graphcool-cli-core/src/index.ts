import Trolo from './trolo'
import Deploy from './deploy'
import Init from './init'
import Auth from './auth/index'
export const topics = [
  { name: 'trolo', description: 'This is a trolling description' },
  { name: 'deploy', description: 'Deploy local project definition' },
  { name: 'init', description: 'Create a new project' },
  { name: 'auth', description: 'Create account or login' }
]

export const commands = [Trolo, Deploy, Init, Auth]
