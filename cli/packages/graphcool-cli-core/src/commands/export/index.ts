import { Command, flags, Flags } from 'graphcool-cli-engine'
import { Exporter } from './Exporter'
import * as path from 'path'
import chalk from 'chalk'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    ['export-path']: flags.string({
      char: 'e',
      description: 'Path to export .zip file',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'E',
    }),
  }
  async run() {
    const exportPath =
      this.flags['export-path'] || `export-${new Date().toISOString()}.zip`

    if (!exportPath.endsWith('.zip')) {
      throw new Error(`export-path must point to a .zip file`)
    }

    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    const clusterName = this.definition.getClusterName()
    const cluster = this.env.clusterByName(clusterName!, true)
    this.env.setActiveCluster(cluster!)

    await this.export(
      serviceName,
      stage,
      exportPath,
      this.definition.getToken(serviceName, stage),
      this.definition.getWorkspace() || undefined,
    )

    const importCommand = chalk.green.bold(
      `$ graphcool import --data ${exportPath}`,
    )
    this.out.log(`Exported service to ${chalk.bold(exportPath)}
You can import it to a new service with
  ${importCommand}`)
  }

  async export(
    serviceName: string,
    stage: string,
    exportPath: string,
    token?: string,
    workspaceSlug?: string,
  ) {
    const exporter = new Exporter(
      exportPath,
      this.client,
      this.out,
      this.config,
    )

    await exporter.download(serviceName, stage, token, workspaceSlug)
  }
}
