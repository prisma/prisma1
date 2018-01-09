import { Command, flags, Flags } from 'graphcool-cli-engine'
import Docker from '../local/Docker'
import { NoClusterSetError } from '../../errors/NoClusterSetError'
import { ClusterNotFoundError } from '../../errors/ClusterNotFoundError'

export default class ClusterLogs extends Command {
  static topic = 'cluster'
  static command = 'logs'
  static description = 'Output cluster logs'
  static group = 'cluster'
  async run() {
    await this.definition.load(this.flags)

    const clusterName = this.definition.getClusterName()
    if (!clusterName) {
      throw new NoClusterSetError()
    }
    const cluster = this.definition.getCluster()
    if (!cluster) {
      throw new ClusterNotFoundError(clusterName)
    }

    if (cluster.local) {
      const docker = new Docker(
        this.out,
        this.config,
        this.env,
        this.flags.name,
      )
      await docker.logs()
    } else {
      throw new Error(
        'Cluster logs for non-local clusters is not implemented yet',
      )
    }
  }
}
