import { Console } from 'console'

import * as callsites from 'callsites'
import { format } from 'util'

export default class BufferedConsole extends Console {
  static write(buffer: any, type: any, message: any, level?: number) {
    const call = callsites()[level != null ? level : 2]
    const origin = call.getFileName() + ':' + call.getLineNumber()
    buffer.push({ message, origin, type })
    return buffer
  }

  /* tslint:disable-next-line */
  _buffer: any

  constructor() {
    const buffer = []
    super({
      write: message => BufferedConsole.write(buffer, 'log', message),
    } as any)
    this._buffer = buffer
  }

  log() {
    BufferedConsole.write(this._buffer, 'log', format.apply(null, arguments))
  }

  info() {
    BufferedConsole.write(this._buffer, 'info', format.apply(null, arguments))
  }

  warn() {
    BufferedConsole.write(this._buffer, 'warn', format.apply(null, arguments))
  }

  error() {
    BufferedConsole.write(this._buffer, 'error', format.apply(null, arguments))
  }

  getBuffer() {
    return this._buffer
  }
}
