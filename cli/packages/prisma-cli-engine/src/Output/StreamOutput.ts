import * as util from 'util'
import * as path from 'path'
import * as fs from 'fs-extra'
import { Output } from './'
import * as stripAnsi from 'strip-ansi'

export function logToFile(msg: string, logfile: string) {
  try {
    fs.mkdirpSync(path.dirname(logfile))
    fs.appendFileSync(logfile, stripAnsi(msg))
  } catch (err) {
    console.error(err)
  }
}

export default class StreamOutput {
  static startOfLine = true

  output = ''
  stream: NodeJS.WriteStream
  out: Output
  logfile?: string

  constructor(stream: NodeJS.WriteStream, output: Output) {
    this.out = output
    this.stream = stream
  }

  write(msg: string, options: { log?: boolean } = {}) {
    let { startOfLine } = this.constructor as any
    const log = options.log !== false
    if (log) {
      this.writeLogFile(msg, startOfLine)
    }
    // conditionally show timestamp if configured to display
    if (startOfLine && this.displayTimestamps) {
      msg = this.timestamp(msg)
    }
    if (this.out.mock) {
      this.output += msg
    } else {
      this.stream.write(msg)
    }
    startOfLine = msg.endsWith('\n')
  }

  timestamp(msg: string): string {
    return `[${new Date().toISOString()}] ${msg}`
  }

  log(data: string, ...args: any[]) {
    let msg = data ? util.format(data, ...args) : ''
    msg += '\n'
    this.out.action.pause(() => this.write(msg))
  }

  writeLogFile(msg: string, withTimestamp: boolean) {
    if (!this.logfile) {
      return
    }
    msg = withTimestamp ? this.timestamp(msg) : msg
    logToFile(msg, this.logfile)
  }

  get displayTimestamps(): boolean {
    const bin = this.out.config.bin.replace('-', '_').toUpperCase()
    const key = `${bin}_TIMESTAMPS`
    return ['1', 'true'].includes(process.env[key] || '')
  }
}
