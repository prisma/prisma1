import { ActionBase } from './ActionBase'
import { Output } from '../index'
import * as stripAnsi from 'strip-ansi'

export class SpinnerAction extends ActionBase {
  spinner: any
  ansi: any
  frames: any
  frameIndex: number
  output: string | null
  width: number

  constructor(out: Output) {
    super(out)
    this.ansi = require('ansi-escapes')
    this.frames = require('../spinners')[
      process.platform === 'win32' ? 'line' : 'dots2'
    ].frames
    this.frameIndex = 0
    const screen = require('./screen')
    this.width = screen.errtermwidth
  }

  _start() {
    this._reset()
    if (this.spinner) {
      clearInterval(this.spinner)
    }
    this._render()
    const interval: any = (this.spinner = setInterval(
      this._render.bind(this),
      this.out.config.windows ? 500 : 100,
      'spinner',
    ))
    interval.unref()
  }

  _stop() {
    clearInterval(this.spinner)
    this._render()
    this.output = null
  }

  _pause(icon?: string) {
    clearInterval(this.spinner)
    this._reset()
    if (icon) {
      this._render(` ${icon}`)
    }
    this.output = null
  }

  _render(icon?: string) {
    const task = this.task
    if (!task) {
      return
    }
    this._reset()
    const frame = icon === 'spinner' ? ` ${this._frame()}` : icon || ''
    const status = task.status ? ` ${task.status}` : ''
    const dots = task.status ? '' : '...'
    this.output = `${task.action}${status}${dots}${frame}\n`
    this._write(this.output)
  }

  _reset() {
    if (!this.output) {
      return
    }
    const lines = this._lines(this.output)
    this._write(
      this.ansi.cursorLeft + this.ansi.cursorUp(lines) + this.ansi.eraseDown,
    )
    this.output = null
  }

  _frame(): string {
    const frame = this.frames[this.frameIndex]
    this.frameIndex = ++this.frameIndex % this.frames.length
    return this.out.color.prisma(frame)
  }

  _lines(s: string): number {
    return stripAnsi(s)
      .split('\n')
      .map(l => Math.ceil(l.length / this.width))
      .reduce((c, i) => c + i, 0)
  }

  _write(s: string) {
    this.out.stdout.write(s, { log: false })
  }
}
