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
    return `${this.baseUrl}/${serviceName}/${stage}`
  }

  getWSEndpoint(serviceName: string, stage: string) {
    const replacedUrl = this.baseUrl.replace('http', 'ws')
    return `${replacedUrl}/${serviceName}/${stage}`
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
