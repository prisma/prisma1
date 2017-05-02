import {Out} from '../types'

export default class TestOut implements Out {

  write(message: string): void {
    console.log(message)
  }

  writeError(message: string): void {
    console.error(message)
  }

  startSpinner(message: string) {
  }

  stopSpinner() {
  }

}