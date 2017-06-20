import {Resolver} from '../../src/types'
import {projectFileSuffix, schemaFileSuffix} from '../../src/utils/constants'

export default class TestResolver implements Resolver {

  storage: { [key: string] : string }

  constructor(storage: { [key: string] : string }) {
    this.storage = storage
  }

  read(path: string): string {
    // absolute path
    if (path.startsWith('/')) {
      return this.storage[path]
    }

    // prepend ./ if necessary
    if (!path.startsWith('./')) {
      path = `./${path}`
    }

    return this.storage[path]
  }

  write(path: string, value: string) {
    // absolute path
    if (path.startsWith('/')) {
      this.storage[path] = value
      return
    }

    // prepend ./ if necessary
    if (!path.startsWith('./')) {
      path = `./${path}`
    }

    this.storage[path] = value
  }

  delete(path: string) {
    // absolute path
    if (path.startsWith('/')) {
      delete this.storage[path]
      return
    }

    // prepend ./ if necessary
    if (!path.startsWith('./')) {
      path = `./${path}`
    }

    delete this.storage[path]
  }

  exists(path: string): boolean {
    return (typeof(this.storage[path]) !== 'undefined') || (typeof(this.storage[`./${path}`]) !== 'undefined')
  }

  projectFiles(directory?: string): string[] {
    const path = directory || '.'
    const files = this.readDirectory(path)
    return files.filter(file => file.endsWith(projectFileSuffix))
  }

  schemaFiles(directory?: string): string[] {
    const path = directory || '.'
    const files = this.readDirectory(path)
    return files.filter(file => file.endsWith(schemaFileSuffix))
  }

  readDirectory(path: string): string[] {
    const paths = Object.keys(this.storage)
    return paths.filter(fullPath => fullPath.startsWith(path))
  }

}

