// import * as path from 'path'
// import * as fs from 'fs-extra'
// import flatten from 'lodash.flatten'
import Trolo from './trolo'


export const topics = [
  { name: 'trolo', description: 'This is a troling description' }
]
//
//
// function getCommands (dir) {
//   function requireCommand (f) {
//     // $FlowFixMe
//     let c = require(f)
//     return c.default ? c.default : c
//   }
//
//   const all = fs.readdirSync(dir).map(f => path.join(dir, f))
//   const foundCommands = all
//     .filter(f => path.extname(f) === '.js' && !f.endsWith('.test.js'))
//     .map(requireCommand)
//   const subs = all
//     .filter(f => fs.lstatSync(f).isDirectory())
//     .map(getCommands)
//   return flatten(foundCommands.concat(flatten(subs)))
// }
//
// export const commands = getCommands(path.join(__dirname, 'commands'))
export const commands = [Trolo]
