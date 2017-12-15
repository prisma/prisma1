import { Command, flags, Flags } from 'graphcool-cli-engine'
import { Exporter } from './Exporter'
import * as path from 'path'
import chalk from 'chalk'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    stage: flags.string({
      char: 's',
      description: 'Stage name',
    }),
    ['export-path']: flags.string({
      char: 'e',
      description: 'Path to export .zip file',
    }),
  }
  async run() {
    const exportPath =
      this.flags['export-path'] || `export-${new Date().toISOString()}.zip`

    if (!exportPath.endsWith('.zip')) {
      throw new Error(`export-path must point to a .zip file`)
    }

    const { stage } = this.flags

    await this.definition.load(this.flags)
    const clusterName = this.definition.getStage(stage, true)
    const stageName = stage || this.definition.rawStages.default
    const cluster = this.env.clusterByName(clusterName!, true)!
    const serviceName = this.definition.definition!.service

    this.env.setActiveCluster(cluster)

    await this.export(
      serviceName,
      stageName,
      exportPath,
      this.definition.getToken(serviceName, stageName),
    )

    const importCommand = chalk.green.bold(
      `$ graphcool import --source ${exportPath} --target target-name`,
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
  ) {
    const exporter = new Exporter(
      exportPath,
      this.client,
      this.out,
      this.config,
    )

    await exporter.download(serviceName, stage, token)
  }
}
