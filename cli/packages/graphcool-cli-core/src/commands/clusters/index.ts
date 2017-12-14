import { Command } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'
const debug = require('debug')('command')

export interface Cluster {
  name: string
  shared: string
}

export default class Clusters extends Command {
  static topic = 'clusters'
  static description = 'List all clusters'
  static group = 'general'
  async run() {
    const clusters: Cluster[] = this.env.clusters.map(c => ({
      name: c.name,
      shared: String(!c.local),
    }))

    this.out.table(clusters)
  }
}
