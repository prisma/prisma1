import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { endpointsMessage } from '../../util'

export default class Info extends Command {
  static topic = 'info'
  static description = 'Print project info (environments, endpoints, ...) '
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set'
    }),
  }
  async run() {
    let {env} = this.flags

    env = env || this.env.env.default

    const {projectId, envName} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(`Please provide a valid environment that has a valid project id`)
    } else {
      const info = await this.client.fetchProjectInfo(projectId)
      this.out.log(`\
${chalk.bold('Environment')}

default: ${envName}
projectId: ${projectId}
`)
      this.out.log(endpointsMessage(projectId))
    }
  }
}
