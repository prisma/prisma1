import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from '../import/Importer'
import { Seeder } from './Seeder'

export default class Seed extends Command {
  static topic = 'seed'
  static description = 'Seed a service with data'
  static flags: Flags = {
    stage: flags.string({
      char: 's',
      description: 'Stage name',
    }),
    reset: flags.boolean({
      char: 'r',
      description: 'Reset the service before seeding',
    }),
  }
  async run() {
    const { stage, reset } = this.flags
    await this.definition.load(this.flags)
    const clusterName = this.definition.getStage(stage, true)
    const stageName = stage || this.definition.rawStages.default
    const cluster = this.env.clusterByName(clusterName!, true)!
    const serviceName = this.definition.definition!.service
    this.env.setActiveCluster(cluster)

    const seeder = new Seeder(
      this.definition,
      this.client,
      this.out,
      this.config,
    )

    await seeder.seed(serviceName, stageName, reset)
  }
}
