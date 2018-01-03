import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class ConsoleCommand extends Command {
  static topic = 'console'
  static description = 'Open Graphcool Console in browser'
  static flags: Flags = {
    web: flags.boolean({
      char: 'w',
      description: 'Force open web playground',
    }),
  }
  async run() {
    const { web } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service!
    const stage = this.definition.definition!.stage
    const cluster = await this.client.getClusterSafe(serviceName, stage)

    opn(`https://console.graph.cool`)
    // if (!cluster.local) {

    // }
  }
}
