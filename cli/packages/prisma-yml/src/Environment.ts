import { Args } from './types/common'
import { Cluster } from './Cluster'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import { ClusterNotFound } from './errors/ClusterNotFound'
import { Variables } from './Variables'
import { IOutput, Output } from './Output'
import * as path from 'path'
import * as os from 'os'
import chalk from 'chalk'
import 'isomorphic-fetch'
import { RC } from './index'
import { ClusterNotSet } from './errors/ClusterNotSet'
const debug = require('debug')('Environment')

const isDev = (process.env.ENV || '').toLowerCase() === 'dev'

export class Environment {
  sharedClusters: string[] = ['prisma-eu1', 'prisma-us1']
  clusterEndpointMap: { [key: string]: string } = {
    'prisma-eu1': 'https://graphcool-eu1.graphcool.cloud',
    'prisma-us1': 'https://graphcool-us1.graphcool.cloud',
  }
  args: Args
  activeCluster: Cluster
  globalRC: RC = {}
  clusters: Cluster[]
  out: IOutput
  home: string
  rcPath: string
  constructor(home: string, out: IOutput = new Output()) {
    this.out = out
    this.home = home

    this.rcPath = path.join(this.home, '.prisma/config.yml')
    fs.mkdirpSync(path.dirname(this.rcPath))
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
        body: JSON.stringify({
          query: `
        {
          publicClusters {
            name
            endpoint
            description
          }
        }
        `,
        }),
      })
      const json = await res.json()
      if (
        json &&
        json.data &&
        json.data.publicClusters &&
        Array.isArray(json.data.publicClusters) &&
        json.data.publicClusters.length > 0
      ) {
        this.sharedClusters = json.data.publicClusters.map(c => c.name)
        this.clusterEndpointMap = json.data.publicClusters.reduce(
          (acc, curr) => {
            return {
              ...acc,
              [curr.name]: curr.endpoint,
            }
          },
          {},
        )

        debug(this.sharedClusters)
        debug(this.clusterEndpointMap)
      }
    } catch (e) {
      debug(e)
      //
    }
  }

  clusterByName(name: string, throws: boolean = false): Cluster | undefined {
    const cluster = this.clusters.find(c => c.name === name)
    if (!throws) {
      return cluster
    }

    if (!cluster) {
      if (!name) {
        throw new ClusterNotSet()
      }
      throw new ClusterNotFound(name)
    }

    return cluster
  }

  setToken(token: string | undefined) {
    this.globalRC.cloudSessionKey = token
  }

  addCluster(cluster: Cluster) {
    const existingClusterIndex = this.clusters.findIndex(
      c => c.name === cluster.name,
    )
    if (existingClusterIndex > -1) {
      this.clusters.splice(existingClusterIndex, 1)
    }
    this.clusters.push(cluster)
  }

  removeCluster(name: string) {
    this.clusters = this.clusters.filter(c => c.name !== name)
  }

  saveGlobalRC() {
    const rc = {
      cloudSessionKey: this.globalRC.cloudSessionKey,
      clusters: this.getLocalClusterConfig(),
    }
    // parse & stringify to rm undefined for yaml parser
    debug('saving global rc')
    debug(rc)
    const rcString = yaml.safeDump(JSON.parse(JSON.stringify(rc)))
    fs.writeFileSync(this.rcPath, rcString)
  }

  setActiveCluster(cluster: Cluster) {
    this.activeCluster = cluster
  }

  async loadGlobalRC(): Promise<void> {
    const globalFile =
      this.rcPath && fs.pathExistsSync(this.rcPath)
        ? fs.readFileSync(this.rcPath, 'utf-8')
        : undefined
    await this.parseGlobalRC(globalFile)
  }

  async parseGlobalRC(globalFile?: string): Promise<void> {
    if (globalFile) {
      this.globalRC = await this.loadYaml(globalFile, this.rcPath)
    }
    this.clusters = this.initClusters(this.globalRC)
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

  private initClusters(rc: RC): Cluster[] {
    const rcClusters = this.getClustersFromRC(rc)
    const sharedClusters = this.getSharedClusters(rc)
    return [...rcClusters, ...sharedClusters]
  }

  private getSharedClusters(rc: RC): Cluster[] {
    return this.sharedClusters.map(clusterName => {
      return new Cluster(
        clusterName,
        this.clusterEndpointMap[clusterName],
        rc && rc.cloudSessionKey,
        false,
      )
    })
  }

  private getClustersFromRC(rc: RC): Cluster[] {
    if (!rc.clusters) {
      return []
    }
    return Object.keys(rc.clusters).map(name => {
      const cluster = rc.clusters![name]
      return new Cluster(name, cluster.host, cluster.clusterSecret, true)
    })
  }

  private getLocalClusterConfig() {
    return this.clusters.filter(c => c.local).reduce((acc, cluster) => {
      return {
        ...acc,
        [cluster.name]: {
          host: cluster.baseUrl,
          clusterSecret: cluster.clusterSecret,
        },
      }
    }, {})
  }
}
