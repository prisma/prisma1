import { Out } from '../../src/types'
import * as fs from 'fs'
import * as chalk from 'chalk'
import {setDebugMessage, contactUsInSlackMessage} from '../../src/utils/constants'
import figures = require('figures')

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
    const message = `${separator}\n${description}\n${separator}\n${command}\n`
    fs.appendFileSync('test.out', message)
  }

  onError(error: Error): void {
    // prevent the same error output twice
    const errorMessage = `Error: ${error.message}`
    if (error.stack && !error.stack.startsWith(errorMessage!)) {
      const message = `${chalk.red(figures.cross)}  Error: ${errorMessage}\n`
      this.write(`${message}\n`)
    } else {
      const errorLines = error.stack!.split('\n')
      const firstErrorLine = errorLines[0]
      const message = `${chalk.red(figures.cross)}  ${firstErrorLine}`
      this.write(`${message}\n`)
    }

    this.write(`\n${setDebugMessage}\n${contactUsInSlackMessage}\n`)
  }

}
