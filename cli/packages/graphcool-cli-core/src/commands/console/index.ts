import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as opn from 'opn'
import {consoleURL} from '../../util'

export default class Console extends Command {
  static topic = 'console'
  static description = 'Open Graphcool Console in browser'
  static group = 'platform'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name'
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let {target} = this.flags

    const foundTarget = await this.env.getTarget(target)
    if (!this.env.isSharedCluster(foundTarget.cluster)) {
      this.out.error(`Can't open the console for the local cluster ${foundTarget.cluster}.
The console is only available in the hosted version of Graphcool.`)
    } else {
      const projectInfo = await this.client.fetchProjectInfo(foundTarget.id)
      opn(consoleURL(this.env.token!, projectInfo.name))
    }
  }
}
