import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as ncp from 'copy-paste'

export default class ClusterToken extends Command {
  static topic = 'cluster-token'
  static description = 'Create a new cluster token'
  static flags: Flags = {
    copy: flags.boolean({
      char: 'c',
      description: 'Copy token to clipboard',
    }),
  }
  async run() {
    const { copy } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.service!
    const stage = this.definition.stage!
    const cluster = this.definition.getCluster()

    if (!cluster) {
      throw new Error(`Please provide a cluster in your prisma.yml`)
    }

    const token = await cluster!.getToken(
      serviceName,
      this.definition.getWorkspace() || undefined,
      stage,
    )

    if (copy) {
      await new Promise(r => {
        ncp.copy(token, () => r())
      })
      this.out.log(`Token copied to clipboard`)
    } else {
      this.out.log(token)
    }
  }
}
