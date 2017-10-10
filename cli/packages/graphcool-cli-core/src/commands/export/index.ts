import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
  }
  async run() {
    const { target } = this.flags
    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(target)

    this.out.action.start('Exporting project')
    const url = await this.client.exportProjectData(id)
    this.out.action.stop()
    this.out
      .log(`You can download your project data by pasting this URL in a browser:
 ${url}
`)
  }
}
