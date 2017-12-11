import { Args } from './types/common'
import { Cluster } from './Cluster'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import { InternalRC } from './types/rc'
import { ClusterNotFound } from './errors/ClusterNotFound'
import { Variables } from './Variables'
import { IOutput, Output } from './Output'

export class Environment {
  sharedClusters: string[] = ['shared-eu-west-1']
  sharedEndpoint = 'https://database-beta.graph.cool'
  args: Args
  activeCluster: Cluster
  globalRC: InternalRC = {}
  clusters: Cluster[]
  platformToken?: string
  globalRCPath: string
  out: IOutput
  constructor(globalRCPath: string, out: IOutput = new Output()) {
    this.globalRCPath = globalRCPath
    this.out = out
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
    fs.writeFileSync(this.globalRCPath, rcString)
  }

  setActiveCluster(cluster: Cluster) {
    this.activeCluster = cluster
  }

  async loadGlobalRC(): Promise<void> {
    const globalFile =
      this.globalRCPath && fs.pathExistsSync(this.globalRCPath)
        ? fs.readFileSync(this.globalRCPath, 'utf-8')
        : undefined
    await this.parseGlobalRC(globalFile)
  }

  async parseGlobalRC(globalFile?: string): Promise<void> {
    if (globalFile) {
      this.globalRC = await this.loadYaml(globalFile, this.globalRCPath)
    }
    this.clusters = this.initClusters(this.globalRC)
    this.platformToken =
      this.globalRC.platformToken || process.env.GRAPHCOOL_PLATFORM_TOKEN
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
        throw new Error(`Yaml parsing error in ${filePath}: ${e.message}`)
      }
      const variables = new Variables(
        filePath || 'no filepath provided',
        this.args,
        this.out,
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
    return this.sharedClusters.map(clusterName => {
      return new Cluster(
        clusterName,
        this.sharedEndpoint,
        rc && rc.platformToken,
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
