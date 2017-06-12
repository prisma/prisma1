import { Out } from '../types'
import { setDebugMessage, contactUsInSlackMessage } from '../utils/constants'
import { makePartsEnclodesByCharacterBold } from '../utils/utils'
import * as chalk from 'chalk'
import figures = require('figures')
import Raven = require('raven')
import ora = require('ora')

const debug = require('debug')('graphcool')

Raven.config('https://6ef6eea3afb041f2aca71d08742a36d1:51bdc5643a7648ffbfb3d3017879467c@sentry.io/178603')

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
