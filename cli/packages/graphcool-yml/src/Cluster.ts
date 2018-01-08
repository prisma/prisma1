const debug = require('debug')('environment')
import 'isomorphic-fetch'
import * as jwt from 'jsonwebtoken'
import { cloudApiEndpoint } from './constants'
import { GraphQLClient } from 'graphql-request'

export class Cluster {
  name: string
  baseUrl: string
  local: boolean
  clusterSecret?: string
  requiresAuth: boolean
  private cachedToken?: string
  constructor(
    name: string,
    baseUrl: string,
    clusterSecret?: string,
    local: boolean = true,
  ) {
    this.name = name
    this.baseUrl = baseUrl
    this.clusterSecret = clusterSecret
    this.local = local
  }

  async getToken(
    serviceName: string,
    workspaceSlug?: string,
    stageName?: string,
  ): Promise<string> {
    if (!this.clusterSecret) {
      return ''
    }
    // public clusters just take the token
    if (this.local) {
      return this.getLocalToken()
    } else {
      return this.generateClusterToken(serviceName, workspaceSlug, stageName)
    }
  }

  getLocalToken() {
    if (!this.cachedToken) {
      const grants = [{ target: `*/*/*`, action: '*' }]

      this.cachedToken = jwt.sign({ grants }, this.clusterSecret, {
        expiresIn: '10 minutes',
        algorithm: 'RS256',
      })
    }

    return this.cachedToken!
  }

  get cloudClient() {
    return new GraphQLClient(cloudApiEndpoint, {
      headers: {
        Authorization: `Bearer ${this.clusterSecret}`,
      },
    })
  }

  async generateClusterToken(
    serviceName: string,
    workspaceSlug: string = '*',
    stageName?: string,
  ): Promise<string> {
    const query = `
      mutation ($input: GenerateClusterTokenRequest!) {
        generateClusterToken(input: $input) {
          clusterToken
        }
      }
    `

    const {
      generateClusterToken: { clusterToken },
    } = await this.cloudClient.request<{
      generateClusterToken: {
        clusterToken: string
      }
    }>(query, {
      input: {
        workspaceSlug,
        clusterName: this.name,
        serviceName,
        stageName,
      },
    })

    return clusterToken
  }

  getApiEndpoint(serviceName: string, stage: string, workspaceSlug?: string) {
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${this.baseUrl}/${workspaceString}${serviceName}/${stage}`
  }

  getWSEndpoint(serviceName: string, stage: string, workspaceSlug?: string) {
    const replacedUrl = this.baseUrl.replace('http', 'ws')
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${replacedUrl}/${workspaceString}${serviceName}/${stage}`
  }

  getImportEndpoint(serviceName: string, stage: string) {
    return `${this.baseUrl}/${serviceName}/${stage}/import`
  }

  getExportEndpoint(serviceName: string, stage: string) {
    return `${this.baseUrl}/${serviceName}/${stage}/export`
  }

  getDeployEndpoint() {
    return `${this.baseUrl}/cluster`
  }

  async isOnline(): Promise<boolean> {
    const version = await this.getVersion()
    return Boolean(version)
  }

  async getVersion(): Promise<string | null> {
    try {
      const result = await fetch(this.getDeployEndpoint(), {
        method: 'post',
        headers: {
          'Content-Type': 'application/json',
        } as any,
        body: JSON.stringify({
          query: `{
            clusterInfo {
              version
            }
          }`,
        }),
      })

      const { data } = await result.json()
      return data.clusterInfo.version
    } catch (e) {
      //
    }

    return null
  }

  // subscriptionEndpoint(projectId: string): string {
  //   if (this.isSharedCluster(this.activeCluster)) {
  //     const region = this.getRegionFromCluster(this.activeCluster)
  //     return this.subscriptionURL({ region, projectId })
  //   }

  //   const match = this.clusterEndpoint.match(/https?:\/\/(.*):(\d+)\/?.*/)
  //   const localAddr = match ? match[1] : 'localhost'
  //   const localPort = match ? match[2] : '60000'
  //   return this.subscriptionURL({ localAddr, localPort, projectId })
  // }
}
