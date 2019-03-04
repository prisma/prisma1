import * as klaw from 'klaw-sync'

export const topics = [
  { name: 'help', description: 'Print available commands and usage' },
  { name: 'version', description: 'Print version' },
]

export const commands = klaw(__dirname, { nodir: true })
  .filter(f => f.path.endsWith('.js'))
  .filter(f => !f.path.endsWith('.test.js'))
  .filter(f => f.path !== __filename)
  .map(f => require(f.path))
