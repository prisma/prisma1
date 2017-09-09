// import * as path from 'path'
// import * as fs from 'fs-extra'
// import flatten from 'lodash.flatten'
import Trolo from './trolo'
import Deploy from './deploy'


export const topics = [
  { name: 'trolo', description: 'This is a trolling description' },
  { name: 'deploy', description: 'Deploy local project definition' },
]

export const commands = [Trolo, Deploy]
