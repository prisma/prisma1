import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from '../import/Importer'
import { Seeder } from './Seeder'

export default class Seed extends Command {
  static topic = 'seed'
  static description = 'Seed a service with data specified in the prisma.yml'
  static flags: Flags = {
    reset: flags.boolean({
      char: 'r',
      description: 'Reset the service before seeding',
    }),
  }
  async run() {
    const { reset } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.service!

    const cluster = this.definition.getCluster()
    this.env.setActiveCluster(cluster!)

    const seeder = new Seeder(
      this.definition,
      this.client,
      this.out,
      this.config,
    )

    await seeder.seed(serviceName, this.definition.stage!, reset)
  }
}
