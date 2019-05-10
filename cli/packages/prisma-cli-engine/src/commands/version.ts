import { Command } from '../Command'

export default class Version extends Command {
  static topic = 'version'
  static description = 'show CLI version'
  static aliases = ['-v', 'v', '--version']
  static printVersionSyncWarning = true

  async run() {
    const { inSync, serverVersion } = await this.areServerAndCLIInSync(this)
    if (inSync) {
      this.out.log(`${this.printCLIVersion()}${this.printServerVersion(serverVersion)}`)
    }
  }
}
