import { Command } from '../Command'

export default class Version extends Command {
  static topic = 'version'
  static description = 'show CLI version'
  static aliases = ['-v', 'v', '--version']

  async run() {
    this.out.log(this.config.userAgent)
  }
}
