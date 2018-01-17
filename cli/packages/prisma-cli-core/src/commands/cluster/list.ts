import { Command } from 'prisma-cli-engine'
import { table, getBorderCharacters } from 'table'
const debug = require('debug')('command')

export default class ClusterList extends Command {
  static topic = 'cluster'
  static command = 'list'
  static description = 'List all clusters'
  static group = 'cluster'
  async run() {
    const clusters = await Promise.all(
      this.env.clusters
        .filter(c => c.name !== 'shared-public-demo')
        .map(async c => ({
          name: c.name,
          version: await c.getVersion(),
          endpoint: c.getDeployEndpoint(),
        })),
    )

    this.out.table(clusters)
  }
}
