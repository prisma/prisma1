import * as yaml from 'js-yaml'
import * as path from 'path'
import fs from './fs'
import { Output } from './Output/index'
import { Client } from './Client/Client'
import { Config } from './Config'
import { EnvDoesntExistError } from './errors/EnvDoesntExistError'
import { Cluster, RC, Target, Targets } from './types/rc'
import { mapValues, merge } from 'lodash'
import { Args } from './types/common'
import Variables from './ProjectDefinition/Variables'
const debug = require('debug')('environment')

const defaultRC = {
  clusters: {
    default: 'shared-eu-west-1'
  }
}

export class Environment {
  localRC: RC = {}
  globalRC: RC = {}
  out: Output
  config: Config
  args: Args
  activeCluster: string = 'shared-eu-west-1'

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
    return merge({}, this.globalRC, this.localRC)
  }

  get token(): string {
    if (this.config.sharedClusters.includes(this.activeCluster)) {
      return this.rc.platformToken!
    }

    return (this.rc.clusters![this.activeCluster]! as Cluster).token
  }

  get default(): Target | null {
    if (this.rc.targets && this.rc.targets.default) {
      return this.rc.targets.default
    }

    return null
  }

  setActiveCluster(cluster: string) {
    this.activeCluster = cluster
  }

  /**
   * This is used to migrate the old .graphcool and .graphcoolrc to the new format
   */
  migrateOldFormat() {
    const dotFilePath = path.join(this.config.home, '.graphcool')
    const dotExists = fs.pathExistsSync(dotFilePath)
    const rcHomePath = path.join(this.config.home, '.graphoolrc')
    const rcHomeExists = fs.pathExistsSync(rcHomePath)

    const dotFile = dotExists ? fs.readFileSync(dotFilePath, 'utf-8') : null
    const rcFile = rcHomeExists ? fs.readFileSync(rcHomePath, 'utf-8') : null

    // if both legacy files exist, prefer the newer one, .graphcool
    if (rcHomeExists) {
      this.out.warn(`Moved deprecated file ${rcHomePath} to .graphcoolrc.old`)
      fs.moveSync(rcHomePath, path.join(this.config.home, '.graphcoolrc.old'))
    }
    if (dotExists) {
      if (dotFile) {
        try {
          const dotJson = JSON.parse(dotFile)
          if (dotJson.token) {
            const rc = {...defaultRC, platformToken: dotJson.token}
            const rcSerialized = this.serializeRC(rc)
            const oldPath = path.join(this.config.home, '.graphcool.old')
            fs.moveSync(dotFile, oldPath)
            fs.writeFileSync(rcHomePath, rcSerialized)
            this.out.warn(`We detected the old definition format of the ${dotFilePath} file.
It has been renamed to ${oldPath}. The new file is called ${rcHomePath}. Read more about the changes here:
https://github.com/graphcool/graphcool/issues/714
`)
          }
        } catch (e) {
          // noop
          console.error(e)
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
        console.error(e)
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
    this.migrateOldFormat()

    const localFile = this.config.localRCPath && fs.pathExistsSync(this.config.localRCPath) ? fs.readFileSync(this.config.localRCPath, 'utf-8') : null
    const globalFile = this.config.globalRCPath && fs.pathExistsSync(this.config.globalRCPath) ? fs.readFileSync(this.config.globalRCPath, 'utf-8') : null

    await this.loadRCs(localFile, globalFile, args)

    if (process.env.NODE_ENV === 'test') {
      this.setTestToken()
    }
  }

  async loadRCs(localFile: string | null, globalFile: string | null, args: Args = {}): Promise<void> {
    this.args = args

    const localJson = await this.loadYaml(localFile, this.config.localRCPath)
    const globalJson = await this.loadYaml(globalFile, this.config.globalRCPath)

    this.deserializeRCs(localJson, globalJson, this.config.localRCPath, this.config.globalRCPath)
  }

  deserializeRCs(localFile: any, globalFile: any, localFilePath: string | null, globalFilePath: string | null): void {
    let allTargets = {...localFile.targets, ...globalFile.targets}

    // 1. resolve aliases
    // global is not allowed to access local variables
    globalFile.targets = this.resolveTargetAliases(globalFile.targets, globalFile.targets)

    // repeat this 2 times as potentially there could be a deeper indirection
    for(let i = 0; i < 2; i++) {
      // first resolve all aliases
      localFile.targets = this.resolveTargetAliases(localFile.targets, allTargets)

      allTargets = {...localFile.targets, ...globalFile.targets}
    }

    // at this point there should only be targets in the form of shared-eu-west-1/cj862nxg0000um3t0z64ls08
    // 2. convert cluster/id to Target
    localFile.targets = this.deserializeTargets(localFile.targets, localFilePath)
    globalFile.targets = this.deserializeTargets(globalFile.targets, globalFilePath)
    // check if clusters exist
    const allClusters = [...this.config.sharedClusters, ...Object.keys(globalFile.clusters || {}), ...Object.keys(localFile.clusters || {})]
    this.checkClusters(localFile.targets, allClusters, localFilePath)
    this.checkClusters(globalFile.targets, allClusters, globalFilePath)
    this.localRC = localFile
    this.globalRC = globalFile
    if (this.rc.clusters && this.rc.clusters.default) {
      if (!allClusters.includes(this.rc.clusters.default)) {
        this.out.error(`Could not find default cluster ${this.rc.clusters.default}`)
      }
      this.activeCluster = this.rc.clusters.default
    }
  }

  checkClusters(targets: Targets, clusters: string[], filePath: string | null) {
    Object.keys(targets).forEach(key => {
      const target = targets[key]
      if (!clusters.includes(target.cluster)) {
        this.out.error(`Could not find cluster ${target.cluster} defined for target ${key} in ${filePath}`)
      }
    })
  }

  deserializeTargets(targets: {[key: string]: string}, filePath: string | null): Targets {
    return mapValues<string, Target>(targets, target => this.deserializeTarget(target, filePath))
  }

  deserializeTarget(target: string, filePath: string | null): Target {
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
  isTargetAlias = (target: string): boolean => target.split('/').length === 1
  resolveTarget = (
    target: string,
    targets: { [key: string]: string },
  ): string =>
    targets[target] ? this.resolveTarget(targets[target], targets) : target

  serializeRC(rc: RC): string {
    const copy: any = {...rc}
    if (copy.targets) {
      copy.targets = this.serializeTargets(copy.targets)
    }
    return yaml.safeDump(copy)
  }

  serializeTargets(targets: Targets) {
    return mapValues<Target, string>(targets, t => `${t.cluster}/${t.id}`)
  }

  setToken(token: string | undefined) {
    this.globalRC.platformToken = token
  }

  saveLocalRC() {

  }

  saveGlobalRC() {
    const file = this.serializeRC(this.globalRC)
    fs.writeFileSync(this.config.globalRCPath, file)
  }
}
