import { Command, flags, Flags } from 'prisma-cli-engine'
import { Exporter } from './Exporter'
import * as path from 'path'
import chalk from 'chalk'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    ['path']: flags.string({
      char: 'p',
      description: 'Path to export .zip file',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  async run() {
    let exportPath =
      this.flags['path'] || `export-${new Date().toISOString()}.zip`

    if (!exportPath.endsWith('.zip')) {
      exportPath += `.zip`
    }

    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)
    const serviceName = this.definition.service!
    const stage = this.definition.stage!

    if (
      this.definition.definition!.databaseType &&
      this.definition.definition!.databaseType === 'document'
    ) {
      throw new Error('Export is not yet supported for document stores.')
    }

    const cluster = this.definition.getCluster()
    this.env.setActiveCluster(cluster!)

    await this.export(
      serviceName,
      stage,
      exportPath,
      this.definition.getToken(serviceName, stage),
      this.definition.getWorkspace() || undefined,
    )

    const importCommand = chalk.green.bold(
      `$ prisma import --data ${exportPath}`,
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
