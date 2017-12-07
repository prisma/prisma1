import { Output } from './index'
import { Config } from './Config'
import { Args } from './types/common'
import { Cluster } from './Cluster'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import Variables from './GraphcoolDefinition/Variables'
import { InternalRC } from './types/rc'
import { ClusterNotFound } from './errors/ClusterNotFound'

export class Environment {
  out: Output
  config: Config
  args: Args
  activeCluster: Cluster
  globalRC: InternalRC = {}
  clusters: Cluster[]
  platformToken?: string
  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }

  async load(args: Args) {
    await this.loadGlobalRC()
  }

  clusterByName(name: string, throws: boolean = false): Cluster | undefined {
    const cluster = this.clusters.find(c => c.name === name)
    if (!throws) {
      return cluster
    }

    if (!cluster) {
      throw new ClusterNotFound(name)
    }

    return cluster
  }

  setToken(token: string | undefined) {
    this.globalRC.platformToken = token
  }

  addCluster(cluster: Cluster) {
    this.clusters.push(cluster)
  }

  saveGlobalRC() {
    const rc = {
      platformToken: this.globalRC.platformToken,
      clusters: this.getLocalClusterConfig(),
    }
    const rcString = yaml.safeDump(rc)
    fs.writeFileSync(this.config.globalRCPath, rcString)
  }

  setActiveCluster(cluster: Cluster) {
    this.activeCluster = cluster
  }

  private async loadGlobalRC(): Promise<void> {
    const globalFile =
      this.config.globalRCPath && fs.pathExistsSync(this.config.globalRCPath)
        ? fs.readFileSync(this.config.globalRCPath, 'utf-8')
        : null
    if (globalFile) {
      this.globalRC = await this.loadYaml(globalFile, this.config.globalRCPath)
      this.clusters = this.initClusters(this.globalRC)
      this.platformToken =
        this.globalRC.platformToken || process.env.GRAPHCOOL_PLATFORM_TOKEN
    }
  }

  private async loadYaml(
    file: string | null,
    filePath: string | null = null,
  ): Promise<any> {
    if (file) {
      let content
      try {
        content = yaml.safeLoad(file)
      } catch (e) {
        this.out.error(`Yaml parsing error in ${filePath}: ${e.message}`)
      }
      const variables = new Variables(
        this.out,
        filePath || 'no filepath provided',
        this.args,
      )
      content = await variables.populateJson(content)

      return content
    } else {
      return {}
    }
  }

  private getClustersFromRC(rc: InternalRC): Cluster[] {
    if (!rc.clusters) {
      return []
    }
    return Object.keys(rc.clusters).map(name => {
      const cluster = rc.clusters![name]
      return new Cluster(name, cluster.host, cluster.clusterSecret, true)
    })
  }

  private initClusters(rc: InternalRC): Cluster[] {
    const rcClusters = this.getClustersFromRC(rc)
    const sharedClusters = this.getSharedClusters(rc)
    return [...rcClusters, ...sharedClusters]
  }

  private getSharedClusters(rc: InternalRC): Cluster[] {
    return this.config.sharedClusters.map(clusterName => {
      return new Cluster(
        clusterName,
        this.config.sharedEndpoint,
        rc.platformToken!,
        false,
      )
    })
  }

  private getLocalClusterConfig() {
    return this.clusters.filter(c => c.local).reduce((acc, cluster) => {
      return {
        ...acc,
        [cluster.name]: {
          host: cluster.baseUrl,
          clusterSecret: cluster.token,
        },
      }
    }, {})
  }
}
