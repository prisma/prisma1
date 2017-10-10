import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as chalk from 'chalk'

export default class Pull extends Command {
  static topic = 'pull'
  static description = 'Pulls a project [deprecated]'
  static hidden = true
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name'
    }),
  }
  async run() {
    const {target} = this.flags
    await this.auth.ensureAuth()

    const {id} = await this.env.getTarget(target)

    // execute the command
    const info = await this.client.fetchProjectInfo(id)
    this.definition.set(info.projectDefinition)
    this.definition.save()
    this.out.log(chalk.blue.bold(`   Written:`))
    this.out.tree(this.config.definitionDir, false)
  }
}
