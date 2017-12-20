import { Command } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'
const debug = require('debug')('command')

export default class Clusters extends Command {
  static topic = 'clusters'
  static description = 'List all clusters'
  static group = 'general'
  async run() {
    const clusters = await Promise.all(
      this.env.clusters.map(async c => ({
        name: c.name,
        version: await c.getVersion(),
        endpoint: c.getDeployEndpoint(),
      })),
    )

    this.out.table(clusters)
  }
}
