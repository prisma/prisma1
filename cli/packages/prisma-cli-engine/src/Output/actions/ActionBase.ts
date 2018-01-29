import { Output } from '../'
import chalk from 'chalk'
import * as figures from 'figures'

export function shouldDisplaySpinner(out: Output) {
  return (
    !process.env.DEBUG &&
    !out.mock &&
    !out.config.debug &&
    !!process.stdin.isTTY &&
    !!process.stderr.isTTY &&
    !process.env.CI &&
    process.env.TERM !== 'dumb'
  )
}

export interface Task {
  action: string
  status?: string
  active?: boolean
}

export class ActionBase {
  task?: Task
  out: Output

  constructor(out: Output) {
    this.out = out
  }

  start(action: string, status?: string) {
    this.task = {
      action,
      status,
      active: true,
    }
    this._start()
    this.task.active = true
    this.log(this.task)
  }

  stop(msg: string = chalk.green(figures.tick) as any) {
    const task = this.task
    if (!task) {
      return
    }
    this.status = msg
    this._stop()
    task.active = false
    delete this.task
  }

  get status(): string | undefined {
    return this.task ? this.task.status : undefined
  }

  set status(status: string | undefined) {
    const task = this.task
    if (!task) {
      return
    }
    if (task.status === status) {
      return
    }
    this._updateStatus(status || '', task.status)
    task.status = status
    this.log(task)
  }

  pause(fn?: () => any, icon?: string) {
    const task = this.task
    const active = task && task.active
    if (task && active) {
      this._pause(icon)
      task.active = false
    } else {
      if (task && !task.active) {
        this.resume()
      }
    }
    const ret = fn ? fn() : null
    return ret
  }

  log({ action, status }: { action: string; status?: string }) {
    const msg = status ? `${action}... ${status}\n` : `${action}...\n`
    this.out.stderr.writeLogFile(msg, true)
  }

  _start() {
    throw new Error('not implemented')
  }

  _stop() {
    throw new Error('not implemented')
  }

  resume() {
    if (this.task) {
      this.start(this.task.action, this.task.status)
    }
  }

  _pause(icon?: string) {
    throw new Error('not implemented')
  }

  _updateStatus(status: string, prevStatus?: string) {
    // noop
  }
}
