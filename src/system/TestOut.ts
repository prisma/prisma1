import { Out } from '../types'
import * as fs from 'fs'

export default class TestOut implements Out {

  write(message: string): void {
    fs.appendFileSync('test.out', message)
  }

  writeError(message: string): void {
    fs.appendFileSync('test.out', message)
  }

  startSpinner(message: string): void {
  }

  stopSpinner(): void {
  }

  prefix(description: string, command: string): void {
    const separator = '================================================================================'
    const message = `\n${separator}\n${description}\n${separator}\n${command}\n`
    fs.appendFileSync('test.out', message)
  }

}
