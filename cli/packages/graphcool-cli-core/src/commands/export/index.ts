import { Command, flags, Flags } from 'graphcool-cli-engine'
import { Exporter } from './Exporter'
import * as path from 'path'
import chalk from 'chalk'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
    ['export-path']: flags.string({
      char: 'e',
      description: 'Path to export .zip file',
    }),
  }
  async run() {
    const { target } = this.flags
    const exportPath =
      this.flags['export-path'] || `export-${new Date().toISOString()}.zip`

    if (!exportPath.endsWith('.zip')) {
      throw new Error(`export-path must point to a .zip file`)
    }

    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(target)

    await this.export(id, exportPath)

    const importCommand = chalk.green.bold(
      `$ graphcool import --source ${exportPath} --target target-name`,
    )
    this.out.log(`Exported service to ${chalk.bold(exportPath)}
You can import it to a new service with
  ${importCommand}`)
  }

  async export(id: string, exportPath: string) {
    const exporter = new Exporter(
      exportPath,
      this.client,
      this.out,
      this.config,
    )

    await exporter.download(id)
  }
}
