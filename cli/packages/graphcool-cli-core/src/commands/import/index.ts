import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from './Importer'

export default class Import extends Command {
  static topic = 'import'
  static description = 'Import data into a service'
  static flags: Flags = {
    stage: flags.string({
      char: 's',
      description: 'Stage name',
      defaultValue: 'dev',
    }),
    source: flags.string({
      char: 's',
      description: 'Path to zip or folder including data to import',
      required: true,
    }),
  }
  async run() {
    const { stage, source } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const cluster = await this.client.getClusterSafe(serviceName, stage)
    this.env.setActiveCluster(cluster)

    if (!source.endsWith('.zip')) {
      throw new Error(`Source must end with .zip`)
    }

    if (!fs.pathExistsSync(source)) {
      throw new Error(`Path ${source} does not exist`)
    }

    // continue
    await this.import(
      source,
      serviceName,
      stage,
      this.definition.getToken(serviceName, stage),
    )
  }

  async import(
    source: string,
    serviceName: string,
    stage: string,
    token?: string,
  ) {
    await this.definition.load({})
    const typesString = this.definition.typesString!
    const importer = new Importer(
      source,
      typesString,
      this.client,
      this.out,
      this.config,
    )
    await importer.upload(serviceName, stage, token)
  }
}
