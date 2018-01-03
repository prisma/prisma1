export class Output {
  log(...args) {
    console.log(args)
  }
  warn(...args) {
    console.warn(args)
  }
  getErrorPrefix(fileName: string, type: 'error' | 'warning' = 'error') {
    return `[${type.toUpperCase()}] in ${fileName}: `
  }
}

export class TestOutput {
  output: string[]
  constructor() {
    this.output = []
  }
  log(...args) {
    this.output = this.output.concat(args)
  }
  warn(...args) {
    this.output = this.output.concat(args)
  }
  getErrorPrefix(fileName: string, type: 'error' | 'warning' = 'error') {
    return `[${type.toUpperCase()}] in ${fileName}: `
  }
}

export interface IOutput {
  warn: (...args) => void
  log: (...args) => void
  getErrorPrefix: (fileName: string, type?: 'error' | 'warning') => string
}
