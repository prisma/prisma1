import * as fs from 'fs'
import * as path from 'path'
import {Resolver} from '../types'

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

}
