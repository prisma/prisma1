import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class ConsoleCommand extends Command {
  static topic = 'console'
  static description = 'Open Graphcool Console in browser'
  async run() {
    throw new Error(`The new console is coming soon!`)
  }
}
