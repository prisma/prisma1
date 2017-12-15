import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import { Importer } from './Importer'

export default class Import extends Command {
  static topic = 'import'
  static description = 'Import data into a service'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
    source: flags.string({
      char: 's',
      description: 'Path to zip or folder including data to import',
      required: true,
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    const { target, source } = this.flags

    const { id } = await this.env.getTarget(target)

    if (!source.endsWith('.zip')) {
      throw new Error(`Source must end with .zip`)
    }

    if (!fs.pathExistsSync(source)) {
      throw new Error(`Path ${source} does not exist`)
    }

    // continue
    await this.import(source, id)
  }

  async import(source: string, id: string) {
    await this.definition.load({})
    const types = this.definition.definition!.modules[0].definition!.types
    const typesPaths = Array.isArray(types) ? types : [types]
    let typesString = ''
    typesPaths.forEach(typesPath => {
      typesString += fs.readFileSync(typesPath, 'utf-8')
    })
    const importer = new Importer(
      source,
      typesString,
      this.client,
      this.out,
      this.config,
    )
    await importer.upload(id)
  }
}

const mapping = {
  'User.comments': 'User.comments.*.id',
  'Comment.user': 'userId',
}
