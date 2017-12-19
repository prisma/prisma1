const debug = require('debug')('environment')
import 'isomorphic-fetch'

export class Cluster {
  name: string
  baseUrl: string
  local: boolean
  private clusterToken?: string
  constructor(
    name: string,
    baseUrl: string,
    token?: string,
    local: boolean = true,
  ) {
    this.name = name
    this.baseUrl = baseUrl
    this.clusterToken = token
    this.local = local
  }

  get token(): string | undefined {
    return this.clusterToken
  }

  getApiEndpoint(serviceName: string, stage: string) {
    return `${this.baseUrl}/api/${serviceName}/${stage}`
  }

  getImportEndpoint(serviceName: string, stage: string) {
    return `${this.baseUrl}/api/${serviceName}/${stage}/import`
  }

  getExportEndpoint(serviceName: string, stage: string) {
    return `${this.baseUrl}/api/${serviceName}/${stage}/export`
  }

  getDeployEndpoint() {
    return `${this.baseUrl}/system/playground`
  }

  async isOnline() {
    const result = await fetch(this.getDeployEndpoint(), {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: `{
            clusterInfo {
              version
            }
          }`,
      }),
    })

    const text = await result.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      throw new Error(text)
    }
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
