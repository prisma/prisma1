import { Args } from './types/common'
import { Cluster } from './Cluster'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import { InternalRC } from './types/rc'
import { ClusterNotFound } from './errors/ClusterNotFound'
import { Variables } from './Variables'
import { IOutput, Output } from './Output'
import * as path from 'path'
import * as os from 'os'
import chalk from 'chalk'
import 'isomorphic-fetch'

export class Environment {
  sharedClusters: string[] = ['shared-public-demo']
  sharedEndpoint = 'https://database-beta.graph.cool'
  args: Args
  activeCluster: Cluster
  globalRC: InternalRC = {}
  clusters: Cluster[]
  platformToken?: string
  globalConfigPath: string
  out: IOutput
  constructor(globalConfigPath: string, out: IOutput = new Output()) {
    this.globalConfigPath = globalConfigPath
    this.out = out
    this.migrateOldCli(path.join(os.homedir(), '.graphcool'), globalConfigPath)
  }

  async load(args: Args) {
    await Promise.all([this.loadGlobalRC(), this.setSharedClusters()])
  }

  async setSharedClusters() {
    try {
      const res = await fetch('https://stats.graph.cool/', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        } as any,
        body: JSON.stringify({ query: `{publicClusters}` }),
      })
      const json = await res.json()
      this.sharedClusters = json.data.publicClusters
    } catch (e) {
      console.error(e)
    }
  }

  migrateOldCli(oldGraphcoolRcPath: string, globalConfigPath: string) {
    if (fs.pathExistsSync(oldGraphcoolRcPath)) {
      var isFile = fs.lstatSync(oldGraphcoolRcPath).isFile()
      if (isFile) {
        var rc = fs.readFileSync(oldGraphcoolRcPath, 'utf-8')
        fs.removeSync(oldGraphcoolRcPath)
        fs.mkdirpSync(oldGraphcoolRcPath)
        fs.writeFileSync(globalConfigPath, rc)
        this.out.log(
          'Old .graphcool file detected. We just moved it to ' +
            globalConfigPath,
        )
      }
    }
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
    // parse & stringify to rm undefined for yaml parser
    const rcString = yaml.safeDump(JSON.parse(JSON.stringify(rc)))
    fs.writeFileSync(this.globalConfigPath, rcString)
  }

  setActiveCluster(cluster: Cluster) {
    this.activeCluster = cluster
  }

  async loadGlobalRC(): Promise<void> {
    const globalFile =
      this.globalConfigPath && fs.pathExistsSync(this.globalConfigPath)
        ? fs.readFileSync(this.globalConfigPath, 'utf-8')
        : undefined
    await this.parseGlobalRC(globalFile)
  }

  async parseGlobalRC(globalFile?: string): Promise<void> {
    if (globalFile) {
      this.globalRC = await this.loadYaml(globalFile, this.globalConfigPath)
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
