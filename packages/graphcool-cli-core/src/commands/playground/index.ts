import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as opn from 'opn'
import { playgroundURL } from '../../util'
import { InvalidProjectError } from '../../errors/InvalidProjectError'

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open the playground for the current selected project'
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
      this.out.error(new InvalidProjectError())
    } else {
      const projectInfo = await this.client.fetchProjectInfo(projectId)
      opn(playgroundURL(this.config.token!, projectInfo.name))
    }
  }
}
