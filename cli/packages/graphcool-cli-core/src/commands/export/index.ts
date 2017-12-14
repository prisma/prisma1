import { Command, flags, Flags } from 'graphcool-cli-engine'
import { Exporter } from './Exporter'
import * as path from 'path'

export default class Export extends Command {
  static topic = 'export'
  static description = 'Export service data to local file'
  static group = 'data'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
    ['export-dir']: flags.string({
      char: 'e',
      description: 'Export directory',
    }),
  }
  async run() {
    const { target } = this.flags
    const exportDir =
      this.flags['export-dir'] ||
      path.join(this.config.cwd, `export-${new Date().toISOString()}/`)
    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(target)

    await this.export(id, exportDir)
  }

  async export(id: string, exportDir: string) {
    const exporter = new Exporter(exportDir, this.client, this.out)

    await exporter.download(id)
  }
}
