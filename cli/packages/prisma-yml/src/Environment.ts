import { Args } from './types/common'
import { Cluster } from './Cluster'
import * as fs from 'fs-extra'
import * as yaml from 'js-yaml'
import { ClusterNotFound } from './errors/ClusterNotFound'
import { Variables } from './Variables'
import { IOutput, Output } from './Output'
import * as path from 'path'
import 'isomorphic-fetch'
import { RC } from './index'
import { ClusterNotSet } from './errors/ClusterNotSet'
import { clusterEndpointMap } from './constants'
import { getProxyAgent } from './utils/getProxyAgent'
const debug = require('debug')('Environment')

export class Environment {
  sharedClusters: string[] = ['prisma-eu1', 'prisma-us1']
  clusterEndpointMap = clusterEndpointMap
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

  async load(loadClusters: boolean = true) {
    await this.loadGlobalRC()
    if (loadClusters) {
      await this.getClusters()
    }
  }

  get cloudSessionKey(): string | undefined {
    return process.env.PRISMA_CLOUD_SESSION_KEY || this.globalRC.cloudSessionKey
  }

  async renewToken() {
    if (this.cloudSessionKey) {
      try {
        const res = await fetch('https://api.cloud.prisma.sh', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${this.cloudSessionKey}`,
          } as any,
          body: JSON.stringify({
            query: `
          mutation {
            renewToken
          }
        `,
          }),
          proxy: getProxyAgent('https://api.cloud.prisma.sh'),
        } as any)
        const json = await res.json()

        if (json && json.data && json.data.renewToken) {
          this.globalRC.cloudSessionKey = json.data.renewToken
          this.saveGlobalRC()
        }
      } catch (e) {
        debug(e)
      }
    }
  }

  async getClusters() {
    if (this.cloudSessionKey) {
      this.renewToken()
      try {
        const res = (await Promise.race([
          fetch('https://api.cloud.prisma.sh', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${this.cloudSessionKey}`,
            } as any,
            body: JSON.stringify({
              query: `
          {
            me {
              memberships {
                workspace {
                  id
                  slug
                  clusters {
                    id
                    name
                    connectInfo {
                      endpoint
                    }
                    customConnectionInfo {
                      endpoint
                    }
                  }
                }
              }
            }
          }
        `,
            }),
            agent: getProxyAgent('https://api.cloud.prisma.sh'),
          } as any),
          new Promise((_, r) => setTimeout(() => r(), 6000)),
        ])) as any
        if (!res) {
          return
        }
        const json = await res.json()
        if (
          json &&
          json.data &&
          json.data.me &&
          json.data.me.memberships &&
          Array.isArray(json.data.me.memberships)
        ) {
          const euIndex = this.clusters.findIndex(c => c.name === 'prisma-eu1')
          this.clusters.splice(euIndex, 1)
          const usIndex = this.clusters.findIndex(c => c.name === 'prisma-us1')
          this.clusters.splice(usIndex, 1)
          json.data.me.memberships.forEach(m => {
            m.workspace.clusters.forEach(cluster => {
              const endpoint = cluster.connectInfo
                ? cluster.connectInfo.endpoint
                : cluster.customConnectionInfo
                ? cluster.customConnectionInfo.endpoint
                : this.clusterEndpointMap[cluster.name]
              this.addCluster(
                new Cluster(
                  this.out,
                  cluster.name,
                  endpoint,
                  this.globalRC.cloudSessionKey,
                  false,
                  ['prisma-eu1', 'prisma-us1'].includes(cluster.name),
                  !['prisma-eu1', 'prisma-us1'].includes(cluster.name),
                  m.workspace.slug,
                ),
              )
            })
          })
        }
      } catch (e) {
        debug(e)
      }
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
    const existingClusterIndex = this.clusters.findIndex(c => {
      if (cluster.workspaceSlug) {
        return (
          c.workspaceSlug === cluster.workspaceSlug && c.name === cluster.name
        )
      } else {
        return c.name === cluster.name
      }
    })
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
      cloudSessionKey: this.globalRC.cloudSessionKey
        ? this.globalRC.cloudSessionKey.trim()
        : undefined,
      clusters: this.getLocalClusterConfig(),
    }
    // parse & stringify to rm undefined for yaml parser
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
    const sharedClusters = this.getSharedClusters(rc)
    return [...sharedClusters]
  }

  private getSharedClusters(rc: RC): Cluster[] {
    return this.sharedClusters.map(clusterName => {
      return new Cluster(
        this.out,
        clusterName,
        this.clusterEndpointMap[clusterName],
        rc && rc.cloudSessionKey,
        false,
        true,
      )
    })
  }

  private getLocalClusterConfig() {
    return this.clusters
      .filter(
        c =>
          !c.shared && c.clusterSecret !== this.cloudSessionKey && !c.isPrivate,
      )
      .reduce((acc, cluster) => {
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

export const isLocal = hostname =>
  hostname.includes('localhost') || hostname.includes('127.0.0.1')
