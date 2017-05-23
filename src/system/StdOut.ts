import { Out } from '../types'
import ora = require('ora')

import * as chalk from 'chalk'
import {setDebugMessage, contactUsInSlackMessage} from '../utils/constants'
import figures = require('figures')
import {makePartsEnclodesByCharacterBold} from '../utils/utils'
var Raven = require('raven')
const debug = require('debug')('graphcool')

export default class StdOut implements Out {

  spinner: any

  write(message: string): void {
    process.stdout.write(message)
  }

  writeError(message: string): void {
    process.stderr.write(message)
  }

  startSpinner(message: string) {
    this.spinner = ora(message).start()
  }

  stopSpinner() {
    if (this.spinner) {
      this.spinner.stop()
    }
  }

  onError(error: Error): void {
    // Raven.captureException(error)

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
    process.exit(1)
  }


}
