import { Out } from '../types'
import ora = require('ora')

export default class StdOut implements Out {

  spinner: any

  write(message: string): void {
    process.stdout.write(message)
  }

  startSpinner(message: string) {
    this.spinner = ora(message).start()
  }

  stopSpinner() {
    if (this.spinner) {
      this.spinner.stop()
    }
  }

}