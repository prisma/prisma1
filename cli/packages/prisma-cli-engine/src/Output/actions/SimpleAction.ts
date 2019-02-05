import { ActionBase } from './ActionBase'

export class SimpleAction extends ActionBase {
  _start () {
    const task = this.task
    if (!task) {
      return
    }
    this._render(task.action, task.status)
  }

  _pause (icon?: string) {
    if (icon) {
      this._updateStatus(icon)
    }
    this._write('\n')
  }

  _resume () {
    // noop
  }

  _updateStatus (status: string, prevStatus?: string) {
    const task = this.task
    if (!task) {return}
    if (task.active && !prevStatus){
      this._write(` ${status}`)
    } else {
      this._render(task.action, status)
    }
  }

  _stop () {
    this._write('\n')
  }

  _render (action: string, status?: string) {
    const task = this.task
    if (!task){
      return
    }
    if (task.active){
      this._write('\n')
    }
    this._write(status ? `${action}... ${status}` : `${action}...`)
  }

  _write (s: string) {
    this.out.stdout.write(s, {log: false})
  }
}
