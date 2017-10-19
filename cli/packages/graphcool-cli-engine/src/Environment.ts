import * as yaml from 'js-yaml'
import * as path from 'path'
import fs from './fs'
import { Output } from './Output/index'
import { Config } from './Config'
import { Cluster, InternalRC, RC, Target, Targets } from './types/rc'
import { mapValues, merge } from 'lodash'
import { Args, Region } from './types/common'
import Variables from './ProjectDefinition/Variables'
const debug = require('debug')('environment')
import * as stringSimilarity from 'string-similarity'
import chalk from 'chalk'

const defaultRC = {
  clusters: {
    default: 'shared-eu-west-1'
  }
}

export class Environment {
  localRC: InternalRC = {}
  globalRC: InternalRC = {}
  out: Output
  config: Config
  args: Args
  activeCluster: string = 'shared-eu-west-1'

  warningCache: {[key: string]: boolean} = {}

  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
    this.migrateOldFormat()
  }
  private setTestToken() {
    debug('taking graphcool test token')
    this.globalRC.platformToken = process.env.GRAPHCOOL_TEST_TOKEN!
  }
  get rc(): RC {
    // todo: memoizing / caching
    return this.deserializeRCs(this.localRC, this.globalRC, this.config.localRCPath, this.config.globalRCPath)
  }

  get token(): string {
    if (process.env.GRAPHCOOL_PLATFORM_TOKEN) {
      debug('taking process.env.GRAPHCOOL_PLATFORM_TOKEN as the token')
      return process.env.GRAPHCOOL_PLATFORM_TOKEN!
    }

    if (this.isSharedCluster(this.activeCluster)) {
      return this.rc.platformToken!
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).clusterSecret
  }

  get default(): Target | null {
    if (this.rc.targets && this.rc.targets.default) {
      return this.rc.targets.default
    }

    return null
  }

  get allClusters(): string[] {
    const localClusters = this.rc.clusters ? Object.keys(this.rc.clusters) : []
    return this.config.sharedClusters.concat(localClusters)
  }

  checkCluster(cluster: string) {
    const allClusters = this.config.sharedClusters.concat(Object.keys(this.rc.clusters || {}))
    if (!allClusters.includes(cluster)) {
      if (cluster === 'local') {
        this.out.log(`You chose the cluster ${chalk.bold('local')}, but don't have docker initialized, yet.
Please run ${chalk.green('$ graphcool local up')} to get a local Graphcool cluster.
`)
        this.out.exit(1)
      }
      const bestMatch = stringSimilarity.findBestMatch(cluster, allClusters).bestMatch.target
      this.out.error(`${cluster} is not a valid cluster. Did you mean ${bestMatch}?`)
    }
  }

  setLocalTarget(name: string, value: string) {
    if (value.split('/').length !== 2) {
      this.out.error(`Invalid target ${name} ${value}`)
    }
    if (!this.localRC.targets) {
      this.localRC.targets = {}
    }
    this.localRC.targets[name] = value
  }

  setLocalDefaultTarget(value: string) {
    if (!this.localRC.targets) {
      this.localRC.targets = {}
    }
    this.localRC.targets.default = value
  }

  getTarget(targetName?: string, silent?: boolean): Target {
    let target: any = null

    if (targetName && targetName.split('/').length > 1) {
      target = this.deserializeTarget(targetName)
    }
    target = targetName || (this.rc.targets && this.rc.targets.default)

    if (typeof target === 'string' && this.rc.targets) {
      target = this.rc.targets[target]
    }

    if (target) {
      this.setActiveCluster(target.cluster)
    } else if (!silent) {
      this.out.error('Please provide a valid target that points to a valid cluster and service id')
    }

    return target
  }

  getTargetWithName(targetName?: string): {target: Target | null, targetName: string | null} {
    let target: any = null
    let name: null | string = null

    if (targetName && targetName.split('/').length > 1) {
      target = this.deserializeTarget(targetName)
    } else {
      if (targetName) {
        target = targetName
        name = targetName
      } else if (this.rc.targets && this.rc.targets.default) {
        target = this.rc.targets.default
        name = (this.localRC.targets && this.localRC.targets.default) || (this.globalRC.targets && this.globalRC.targets.default) || null
        name = (name && name.split('/').length === 1) ? name : null
      }

      if (typeof target === 'string' && this.rc.targets) {
        target = this.rc.targets[target]
      }
    }

    if (target) {
      this.setActiveCluster(target.cluster)
    }

    return {
      target,
      targetName: name,
    }
  }

  getDefaultTargetName(cluster: string) {
    const targetName = this.isSharedCluster(cluster) ? 'prod' : 'dev'
    if (!this.rc.targets || !this.rc.targets[targetName]) {
      return targetName
    } else {
      let count = 1
      while (this.rc.targets[targetName + count]) {
        count++
      }
      return targetName + count
    }
  }

  setActiveCluster(cluster: string) {
    this.checkCluster(cluster)
    this.activeCluster = cluster
  }

  isSharedCluster(cluster: string) {
    return this.config.sharedClusters.includes(cluster)
  }

  deleteIfExist(serviceIds: string[]) {
    serviceIds.forEach(id => {
      const localTarget = Object.keys(this.localRC.targets || {}).find(
        name => this.localRC.targets![name].split('/')[1] === id,
      )
      if (localTarget) {
        delete this.localRC[localTarget]
      }
      const globalTarget = Object.keys(this.globalRC.targets || {}).find(
        name => this.globalRC.targets![name].split('/')[1] === id,
      )
      if (globalTarget) {
        delete this.globalRC[globalTarget]
      }
    })
  }

  /**
   * This is used to migrate the old .graphcool and .graphcoolrc to the new format
   */
  migrateOldFormat() {
    this.migrateGlobalFiles()
    this.migrateLocalFile()
    this.migrateClusters(this.config.localRCPath)
    this.migrateClusters(this.config.globalRCPath)
  }

  migrateLocalFile() {
    if (fs.pathExistsSync(this.config.localRCPath)) {
      const file = fs.readFileSync(this.config.localRCPath, 'utf-8')
      let content
      try {
        content = yaml.safeLoad(file)
        // we got the old format here
        if (content.environments) {
          const newLocalRcJson = {
            targets: mapValues(content.environments, env => {
              return `shared-eu-west-1/${env}`
            }),
            clusters: {
            }
          }
          if (content.default) {
            newLocalRcJson.targets.default = content.default
          }
          const newLocalRcYaml = yaml.safeDump(newLocalRcJson)
          const oldPath = path.join(this.config.cwd, '.graphcoolrc.old')
          fs.moveSync(this.config.localRCPath, oldPath)
          fs.writeFileSync(this.config.localRCPath, newLocalRcYaml)
          this.out.warn(`We detected the old definition format of the ${this.config.localRCPath} file.
It has been renamed to ${oldPath}. The up-to-date format has been written to ${this.config.localRCPath}.
Read more about the changes here:
https://github.com/graphcool/graphcool/issues/714
`)
        }
      } catch (e) {
      }
    }
  }

  migrateClusters(rcPath: string) {
    if (fs.pathExistsSync(rcPath)) {
      const file = fs.readFileSync(rcPath, 'utf-8')
      let content
      try {
        content = yaml.safeLoad(file)
        if (content.clusters && Object.keys(content.clusters).find(c => content.clusters[c].token)) {
          const newRcJson = {
            ...content,
            clusters: mapValues(content.clusters, c => typeof c === 'string' ? c : ({
              clusterSecret: c.token,
              host: c.host,
            }))
          }
          const newLocalRcYaml = yaml.safeDump(newRcJson)
          const oldPath = path.join(path.dirname(rcPath), '.graphcoolrc.old')
          fs.moveSync(rcPath, oldPath)
          fs.writeFileSync(rcPath, newLocalRcYaml)
          this.out.warn(`We detected the old definition format of ${chalk.bold('clusters')} in the ${rcPath} file.
${chalk.bold('token')} has been renamed to ${chalk.bold('clusterSecret')}.
It has been renamed to ${oldPath}. The up-to-date format has been written to ${rcPath}.
`)
        }
        if (content.clusters && Object.keys(content.clusters).find(c => typeof content.clusters[c] !== 'string' && !content.clusters[c].faasHost)) {
          const newRcJson = {
            ...content,
            clusters: mapValues(content.clusters, c => typeof c === 'string' ? c : ({
              ...c,
              faasHost: 'http://localhost:60050'
            }))
          }
          const newLocalRcYaml = yaml.safeDump(newRcJson)
          fs.writeFileSync(rcPath, newLocalRcYaml)
          this.out.warn(`We detected the old definition format of ${chalk.bold('clusters')} in the ${rcPath} file. A new field called ${chalk.bold('faasHost')} has been added, which contains the address to the new local function runtime.`)
        }
      } catch (e) {
      }
    }
  }

  migrateGlobalFiles() {
    const dotFilePath = path.join(this.config.home, '.graphcool')
    const dotExists = fs.pathExistsSync(dotFilePath)
    const rcHomePath = path.join(this.config.home, '.graphcoolrc')
    const rcHomeExists = fs.pathExistsSync(rcHomePath)

    const dotFile = dotExists ? fs.readFileSync(dotFilePath, 'utf-8') : null
    const rcFile = rcHomeExists ? fs.readFileSync(rcHomePath, 'utf-8') : null

    // if both legacy files exist, prefer the newer one, .graphcool
    if (rcHomeExists && rcFile) {
      // only move this file, if it is json and contains the "token" field
      // in this case, it's the old format
      try {
        const rcJson = JSON.parse(rcFile)
        if (Object.keys(rcJson).length === 1 && rcJson.token) {
          this.out.warn(`Moved deprecated file ${rcHomePath} to .graphcoolrc.old`)
          fs.moveSync(rcHomePath, path.join(this.config.home, '.graphcoolrc.old'))
        }
      } catch (e) {
        //
      }
    }
    if (dotExists) {
      if (dotFile) {
        try {
          const dotJson = JSON.parse(dotFile)
          if (dotJson.token) {
            const rc = {...defaultRC, platformToken: dotJson.token}
            const rcSerialized = this.serializeRC(rc)
            const oldPath = path.join(this.config.home, '.graphcool.old')
            fs.moveSync(dotFilePath, oldPath)
            debug(`Writing`, rcHomePath, rcSerialized)
            fs.writeFileSync(rcHomePath, rcSerialized)
            const READ = fs.readFileSync(rcHomePath, 'utf-8')
            debug('YES', READ)
            this.out.warn(`We detected the old definition format of the ${dotFilePath} file.
It has been renamed to ${oldPath}. The new file is called ${rcHomePath}.
Read more about the changes here:
https://github.com/graphcool/graphcool/issues/714
`)
          }
        } catch (e) {
          // noop
        }
      }
    } else if (rcHomeExists && rcFile) {
      try {
        const rcJson = JSON.parse(rcFile)
        const rc = {...defaultRC, platformToken: rcJson.token}
        const rcSerialized = this.serializeRC(rc)
        fs.writeFileSync(rcHomePath, rcSerialized)
      } catch (e) {
        // noop
      }
    }
  }

  async loadYaml(file: string | null, filePath: string | null = null): Promise<any> {
    if (file) {
      let content
      try {
        content = yaml.safeLoad(file)
      } catch (e) {
        this.out.error(`Yaml parsing error in ${filePath}: ${e.message}`)
      }
      const variables = new Variables(this.out, filePath || 'no filepath provided', this.args)
      content = await variables.populateJson(content)

      return content
    } else {
      return {}
    }
  }

  async load(args: Args) {
    const localFile = this.config.localRCPath && fs.pathExistsSync(this.config.localRCPath) ? fs.readFileSync(this.config.localRCPath, 'utf-8') : null
    const globalFile = this.config.globalRCPath && fs.pathExistsSync(this.config.globalRCPath) ? fs.readFileSync(this.config.globalRCPath, 'utf-8') : null

    await this.loadRCs(localFile, globalFile, args)

    if (process.env.NODE_ENV === 'test') {
      this.setTestToken()
    }
  }

  async loadRCs(localFile: string | null, globalFile: string | null, args: Args = {}): Promise<void> {
    this.args = args

    this.localRC = await this.loadYaml(localFile, this.config.localRCPath)
    this.globalRC = await this.loadYaml(globalFile, this.config.globalRCPath)

    if (this.rc.clusters && this.rc.clusters.default) {
      if (!this.allClusters.includes(this.rc.clusters.default)) {
        this.out.error(`Could not find default cluster ${this.rc.clusters.default}`)
      }
      this.activeCluster = this.rc.clusters.default
    }

  }

  deserializeRCs(localFile: any, globalFile: any, localFilePath: string | null, globalFilePath: string | null): RC {
    let allTargets = {...localFile.targets, ...globalFile.targets}
    const newLocalFile = {...localFile}
    const newGlobalFile = {...globalFile}

    // 1. resolve aliases
    // global is not allowed to access local variables
    newGlobalFile.targets = this.resolveTargetAliases(newGlobalFile.targets, newGlobalFile.targets)

    // repeat this 2 times as potentially there could be a deeper indirection
    for(let i = 0; i < 2; i++) {
      // first resolve all aliases
      newLocalFile.targets = this.resolveTargetAliases(newLocalFile.targets, allTargets)

      allTargets = {...newLocalFile.targets, ...newGlobalFile.targets}
    }

    // at this point there should only be targets in the form of shared-eu-west-1/cj862nxg0000um3t0z64ls08
    // 2. convert cluster/id to Target
    newLocalFile.targets = this.deserializeTargets(newLocalFile.targets, localFilePath)
    newGlobalFile.targets = this.deserializeTargets(newGlobalFile.targets, globalFilePath)
    // check if clusters exist
    const allClusters = [...this.config.sharedClusters, ...Object.keys(newGlobalFile.clusters || {}), ...Object.keys(newLocalFile.clusters || {})]
    this.checkClusters(newLocalFile.targets, allClusters, localFilePath)
    this.checkClusters(newGlobalFile.targets, allClusters, globalFilePath)
    return merge({}, newGlobalFile, newLocalFile)
  }

  checkClusters(targets: Targets, clusters: string[], filePath: string | null) {
    Object.keys(targets).forEach(key => {
      const target = targets[key]
      if (!clusters.includes(target.cluster) && !this.warningCache[target.cluster]) {
        this.warningCache[target.cluster] = true
        if (target.cluster === 'local') {
          this.out.warn(`Could not find cluster ${target.cluster} defined for target ${key} in ${filePath}.
Please run ${chalk.bold('graphcool local up')} to start the local cluster.`)
        } else {
          this.out.error(`Could not find cluster ${target.cluster} defined for target ${key} in ${filePath}`)
        }
      }
    })
  }

  deserializeTargets(targets: {[key: string]: string}, filePath: string | null): Targets {
    return mapValues<string, Target>(targets, target => this.deserializeTarget(target, filePath))
  }

  deserializeTarget(target: string, filePath: string | null = null): Target {
    const splittedTarget = target.split('/')
    if (splittedTarget.length === 1) {
      this.out.error(`Could not parse target ${target} in ${filePath}`)
    }
    return {
      cluster: splittedTarget[0],
      id: splittedTarget[1]
    }
  }

  resolveTargetAliases = (targets, allTargets) => mapValues(targets, target =>
    this.isTargetAlias(target) ? this.resolveTarget(target, allTargets) : target
  )
  isTargetAlias = (target: string | Target): boolean => {
    return typeof target === 'string' && target.split('/').length === 1
  }
  resolveTarget = (
    target: string,
    targets: { [key: string]: string },
  ): string =>
    targets[target] ? this.resolveTarget(targets[target], targets) : target

  serializeRC(rc: InternalRC): string {
    // const copy: any = {...rc}
    // if (copy.targets) {
    //   copy.targets = this.serializeTargets(copy.targets)
    // }
    return yaml.safeDump(rc)
  }
  //
  // serializeTargets(targets: Targets) {
  //   return mapValues<Target, string>(targets, t => `${t.cluster}/${t.id}`)
  // }

  setToken(token: string | undefined) {
    this.globalRC.platformToken = token
  }

  saveLocalRC() {
    const file = this.serializeRC(this.localRC)
    fs.writeFileSync(this.config.localRCPath, file)
  }

  saveGlobalRC() {
    const file = this.serializeRC(this.globalRC)
    fs.writeFileSync(this.config.globalRCPath, file)
  }

  save() {
    this.saveLocalRC()
    this.saveGlobalRC()
  }

  setGlobalCluster(name: string, cluster: Cluster) {
    if (!this.globalRC.clusters) {
      this.globalRC.clusters = {}
    }
    this.globalRC.clusters[name] = cluster
  }

  setLocalDefaultCluster(cluster: string) {
    if (!this.globalRC.clusters) {
      this.globalRC.clusters = {}
    }
    this.globalRC.clusters.default = cluster
  }

  getRegionFromCluster(cluster: string): Region {
    if (this.isSharedCluster(cluster)) {
      return cluster.slice(7).replace(/-/g, '_').toUpperCase() as Region
    } else {
      return 'EU_WEST_1'
    }
  }

  get clusterEndpoint(): string {
    if (this.isSharedCluster(this.activeCluster)) {
      return this.config.systemAPIEndpoint
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).host + '/system'
  }

  simpleEndpoint(projectId: string): string {
    if (this.isSharedCluster(this.activeCluster)) {
      return this.config.simpleAPIEndpoint + projectId
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).host + '/simple/v1/' + projectId
  }

  relayEndpoint(projectId: string): string {
    if (this.isSharedCluster(this.activeCluster)) {
      return this.config.relayAPIEndpoint + projectId
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).host + '/relay/v1/' + projectId
  }

  fileEndpoint(projectId: string): string {
    if (this.isSharedCluster(this.activeCluster)) {
      return this.config.fileAPIEndpoint + projectId
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).host + '/file/v1/' + projectId
  }

  subscriptionEndpoint(projectId: string): string {
    if (this.isSharedCluster(this.activeCluster)) {
      const region = this.getRegionFromCluster(this.activeCluster)
      return this.subscriptionURL({region, projectId})
    }

    const match = this.clusterEndpoint.match(/.*:(\d+)\/?.*/)
    const localPort = match ? match[1] : '60000'
    return this.subscriptionURL({localPort, projectId})
  }


  private subscriptionURL = ({region, projectId, localPort}: {region?: Region, projectId: string, localPort?: number | string}) =>
    localPort ? `ws://localhost:${localPort}/subscriptions/v1/${projectId}` :
      `${subscriptionEndpoints[region!]}/v1/${projectId}`
}

const subscriptionEndpoints = {
  EU_WEST_1: 'wss://subscriptions.graph.cool',
  US_WEST_2: 'wss://subscriptions.us-west-2.graph.cool',
  AP_NORTHEAST_1: 'wss://subscriptions.ap-northeast-1.graph.cool',
}
