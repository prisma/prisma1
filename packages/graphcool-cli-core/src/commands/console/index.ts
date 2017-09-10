import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as opn from 'opn'
import {consoleURL} from '../../util'

export default class Console extends Command {
  static topic = 'console'
  static description = 'Open the console for the current selected project'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name'
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let {env} = this.flags

    env = env || this.env.env.default

    const {projectId} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(`Please provide a valid environment that has a valid project id`)
    } else {
      const projectInfo = await this.client.fetchProjectInfo(projectId)
      opn(consoleURL(this.config.token!, projectInfo.name))
    }
  }
}
