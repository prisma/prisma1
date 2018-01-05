import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'

export default class Token extends Command {
  static topic = 'token'
  static description = 'Create a new service token'
  async run() {
    const { reset } = this.flags
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
      this.out.log(token)
    }
  }
}
