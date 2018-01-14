let isGlobal: null | boolean = null
import * as path from 'path'

export function getIsGlobal() {
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
    if (!process.env._) {
      isGlobal = true
    } else if (process.mainModule) {
      const fileName = process.mainModule.filename
      const relative = path.relative(process.cwd(), fileName)
      isGlobal = !relative.startsWith('..')
    }
    isGlobal = process.env._ !== process.execPath
  }

  return isGlobal
}
