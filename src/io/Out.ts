import { setDebugMessage, contactUsInSlackMessage } from '../utils/constants'
import { makePartsEnclodesByCharacterBold } from '../utils/utils'
import * as chalk from 'chalk'
import figures = require('figures')
import Raven = require('raven')
import ora = require('ora')
import * as fs from 'fs'

const debug = require('debug')('graphcool')

class Out {

  spinner: any
  private out: (msg: string) => void
  private err: (msg: string) => void

  constructor() {
    let out: (msg: string) => void
    let err: (msg: string) => void
    if (process.env.NODE_ENV === 'test') {
      out = process.stdout.write
      err = process.stderr.write
    } else {
      out = (message: string) => fs.appendFileSync('test.out', message)
      err = (message: string) => fs.appendFileSync('test.out', message)
    }
    this.out = out
    this.err = err
  }

  write(message: string): void {
    this.out(message)
  }

  writeError(message: string): void {
    this.err(message)
  }

  startSpinner(message: string) {
    if (process.env.NODE_ENV !== 'test') {
      this.spinner = ora(message).start()
    }
  }

  stopSpinner() {
    if (process.env.NODE_ENV !== 'test') {
      if (this.spinner) {
        this.spinner.stop()
      }
    }
  }

  async onError(error: Error): Promise<void> {

    // prevent the same error output twice
    const errorMessage = makePartsEnclodesByCharacterBold(`Error: ${error.message}`, `\``)
    if (error.stack && !error.stack.startsWith(errorMessage!)) {
      console.error(`${chalk.red(figures.cross)}  ${errorMessage}`)
      debug(error.stack)
    } else if (error.stack) {
      const errorLines = error.stack!.split('\n')
      const firstErrorLine = errorLines[0]
      console.error(`${chalk.red(figures.cross)}  ${firstErrorLine}`)
      debug(error.stack)
    } else {
      console.error(JSON.stringify(error))
    }

    console.error(`\n${setDebugMessage}\n${contactUsInSlackMessage}\n`)

    await new Promise(resolve => Raven.captureException(error, resolve))

    process.exit(1)
  }


}

const out = new Out()

export default out
