import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open the playground for the current selected project'
  static group = 'general'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
  }
  async run() {
    // await this.auth.ensureAuth()
    const { target } = this.flags
    const {id} = await this.env.getTarget(target)

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

    if (fs.pathExistsSync(localPlaygroundPath)) {
      childProcess.spawn(
        localPlaygroundPath,
        ['endpoint', this.env.simpleEndpoint(id)],
        { detached: true },
      )
    } else {
      opn(this.env.simpleEndpoint(id))
    }
  }
}
