import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidTargetError'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export project data'
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
      this.out.action.start('Exporting project')
      const url = await this.client.exportProjectData(projectId)
      this.out.action.stop()
      this.out.log(`You can download your project data by pasting this URL in a browser:
 ${url}
`)
    }
  }
}
