export class Output {
  warn(...args) {
    console.warn(args)
  }
  getErrorPrefix(fileName: string, type: 'error' | 'warning' = 'error') {
    return `[${type.toUpperCase()}] in ${fileName}: `
  }
}

export interface IOutput {
  warn: (...args) => void
  getErrorPrefix: (fileName: string, type?: 'error' | 'warning') => string
}
