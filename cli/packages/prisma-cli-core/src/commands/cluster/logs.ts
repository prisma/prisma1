import { Command, flags, Flags } from 'prisma-cli-engine'
import Docker from '../local/Docker'
import { ClusterNotFound, ClusterNotSet } from 'prisma-yml'

export default class ClusterLogs extends Command {
  static topic = 'cluster'
  static command = 'logs'
  static description = 'Output cluster logs'
  static group = 'cluster'
  async run() {
    await this.definition.load(this.flags)

    const clusterName = this.definition.getClusterName()
    if (!clusterName) {
      throw new ClusterNotSet()
    }
    const cluster = this.definition.getCluster()
    if (!cluster) {
      throw new ClusterNotFound(clusterName)
    }

    if (cluster.local) {
      const docker = new Docker(this.out, this.config, this.env, 'local')
      await docker.init()
      await docker.logs()
    } else {
      throw new Error(
        'Cluster logs for non-local clusters is not implemented yet',
      )
    }
  }
}
