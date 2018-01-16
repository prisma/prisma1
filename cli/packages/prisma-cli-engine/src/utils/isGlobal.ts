let isGlobal: null | boolean = null
import * as path from 'path'
const debug = require('debug')('isGlobal')

export function getIsGlobal() {
  debug({ argv: process.argv })
  if (isGlobal !== null) {
    return isGlobal
  }

  isGlobal = false

  if (process.platform === 'win32') {
    if (process.env.Path && process.mainModule) {
      const paths = process.env.Path!.split(';')
      for (const p of paths) {
        if (
          p.indexOf('npm') !== -1 &&
          process.mainModule.filename.indexOf(p) !== -1
        ) {
          isGlobal = true
          break
        }
      }
    }
  } else {
    isGlobal = !process.argv[1].includes('node_modules/.bin')
  }

  return isGlobal
}
