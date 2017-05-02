import * as fs from 'fs'
import * as path from 'path'
import {Resolver} from '../types'
import {projectFileSuffix, schemaFileSuffix} from '../utils/constants'

export default class FileSystemResolver implements Resolver {

  read(fileUrl: string): string {
    return fs.readFileSync(path.resolve(fileUrl)).toString()
  }

  write(fileUrl: string, value: string) {
    fs.writeFileSync(path.resolve(fileUrl), value)
  }

  delete(fileUrl: string) {
    fs.unlinkSync(path.resolve(fileUrl))
  }

  exists(path: string): boolean {
    return fs.existsSync(path)
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
    return fs.readdirSync(path)
  }

}
