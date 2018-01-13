import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from './Importer'

export default class Import extends Command {
  static topic = 'import'
  static description = 'Import data into a service'
  static flags: Flags = {
    data: flags.string({
      char: 'd',
      description: 'Path to zip or folder including data to import',
      required: true,
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  async run() {
    const { data } = this.flags
    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    const clusterName = this.definition.getClusterName()
    const cluster = this.env.clusterByName(clusterName!, true)
    this.env.setActiveCluster(cluster!)

    if (!data.endsWith('.zip')) {
      throw new Error(`data must end with .zip`)
    }

    if (!fs.pathExistsSync(data)) {
      throw new Error(`Path ${data} does not exist`)
    }

    // continue
    await this.import(
      data,
      serviceName,
      stage,
      this.definition.getToken(serviceName, stage),
      this.definition.getWorkspace() || undefined,
    )
  }

  async import(
    source: string,
    serviceName: string,
    stage: string,
    token?: string,
    workspaceSlug?: string,
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
    await importer.upload(serviceName, stage, token, workspaceSlug)
  }
}
