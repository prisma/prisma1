import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as clipboardy from 'clipboardy'

export default class ClusterToken extends Command {
  static topic = 'cluster-token'
  static description = 'Create a new cluster token'
  static flags: Flags = {
    copy: flags.boolean({
      char: 'c',
      description: 'Copy token to clipboard',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
    ['project']: flags.string({
      description: 'Path to Prisma definition file',
      char: 'p',
    }),
  }
  async run() {
    const { copy } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.service!
    const stage = this.definition.stage!
    const cluster = await this.definition.getCluster()

    if (!cluster) {
      throw new Error(`Please provide a cluster in your prisma.yml`)
    }

    const token = await cluster!.getToken(
      serviceName,
      this.definition.getWorkspace() || undefined,
      stage,
    )

    if (!token) {
      throw new Error(`Couldn't generate token`)
    }

    if (copy) {
      await new Promise(r => {
        clipboardy.writeSync(token)
        r()
      })
      this.out.log(`Token copied to clipboard`)
    } else {
      this.out.log(token)
    }
  }
}
