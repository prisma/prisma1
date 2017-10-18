import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open service endpoints in GraphQL Playground'
  static group = 'general'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
    web: flags.boolean({
      char: 'w',
      description: 'Force open web playground',
    }),
  }
  async run() {
    // await this.auth.ensureAuth()
    const { target, web } = this.flags
    const {id} = await this.env.getTarget(target)
    await this.auth.ensureAuth()

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

    if (fs.pathExistsSync(localPlaygroundPath) && !web) {
      childProcess.spawn(
        localPlaygroundPath,
        ['--endpoint', this.env.simpleEndpoint(id), '--platform-token', this.env.token],
        { detached: true },
      )
    } else {
      opn(this.env.simpleEndpoint(id))
    }
  }
}
