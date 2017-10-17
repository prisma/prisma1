import * as path from 'path'
import * as fs from 'fs-extra'

function getPaths(bin: string): string[] {
  const envPath = process.env.PATH || ''
  const envExt = process.env.PATHEXT || ''
  return envPath
    .replace(/["]+/g, '')
    .split(path.delimiter)
    .map(function(chunk) {
      return envExt.split(path.delimiter).map(function(ext) {
        return path.join(chunk, bin + ext)
      })
    })
    .reduce(function(a, b) {
      return a.concat(b)
    })
}

export async function getBinPath(bin: string): Promise<string | null> {
  const paths = getPaths(bin)
  const exist = await Promise.all(paths.map(p => fs.pathExists(p)))
  const existsIndex = exist.findIndex(e => e)
  if (existsIndex > -1) {
    return paths[existsIndex]
  }

  return null
}