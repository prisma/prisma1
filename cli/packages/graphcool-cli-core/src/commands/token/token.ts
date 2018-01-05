import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as ncp from 'copy-paste'

export default class Token extends Command {
  static topic = 'token'
  static description = 'Create a new service token'
  static flags: Flags = {
    copy: flags.boolean({
      char: 'c',
      description: 'Copy token to clipboard',
    }),
  }
  async run() {
    const { copy } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage
    const clusterName = this.definition.definition!.cluster
    const cluster = this.env.clusterByName(clusterName!, true)
    this.env.setActiveCluster(cluster!)

    const token = this.definition.getToken(serviceName, stage)
    if (!token) {
      this.out.log(`There is no secret set in the graphcool.yml`)
    } else {
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
}
