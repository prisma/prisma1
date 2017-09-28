import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'
import * as chalk from 'chalk'

export default class Pull extends Command {
  static topic = 'pull'
  static description = 'Pulls a project [deprecated]'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to set',
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
      // execute the command
      const info = await this.client.fetchProjectInfo(projectId)
      this.definition.set(info.projectDefinition)
      this.definition.save()
      this.out.log(chalk.blue.bold(`   Written:`))
      this.out.tree(this.config.definitionDir, false)
    }
  }
}
