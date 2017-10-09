import * as yaml from 'js-yaml'
import * as path from 'path'
import fs from './fs'
import { Output } from './Output/index'
import { Client } from './Client/Client'
import { Config } from './Config'
import { EnvDoesntExistError } from './errors/EnvDoesntExistError'
import { RC, Target, Targets } from './types/rc'
import { mapValues, merge } from 'lodash'
import { Args } from './types/common'
import Variables from './ProjectDefinition/Variables'
const debug = require('debug')('environment')

export class Environment {
  localRC: RC = {}
  globalRC: RC = {}
  out: Output
  client: Client
  config: Config
  args: Args

  constructor(out: Output, config: Config, client: Client) {
    this.out = out
    this.config = config
    this.client = client
    this.migrateOldFormat()
  }
  private setTestToken() {
    debug('taking graphcool test token')
    this.globalRC.platformToken = process.env.GRAPHCOOL_TEST_TOKEN!
  }

  get rc(): RC {
    return merge({}, this.globalRC, this.localRC)
  }

  get default(): Target | null {
    if (this.rc.targets && this.rc.targets.default) {
      return this.rc.targets.default
    }

    return null
  }

  /**
   * This is used to migrate the old .graphcool and .graphcoolrc to the new format
   */
  migrateOldFormat() {}
  // public loadDotGraphcool() {
  //   if (this.dotGraphcoolFilePath && fs.existsSync(this.dotGraphcoolFilePath)) {
  //     const configContent = fs.readFileSync(
  //       this.dotGraphcoolFilePath,
  //       'utf-8',
  //     )
  //     let file: any = null
  //     try {
  //       file = JSON.parse(configContent)
  //     } catch (e) {
  //       try {
  //         file = yaml.safeLoad(configContent)
  //       } catch (e) {
  //         this.out.error(`Could not load ${this.dotGraphcoolFilePath}. It's neither valid json nor valid yaml.`)
  //       }
  //     }
  //
  //     if (file.token) {
  //       debug(`loading .graphcool file: no token existing`)
  //       this.token = file.token
  //     }
  //
  //     if (file.targets) {
  //       debug(`loading .graphcool file: no targets existing`)
  //       this.targets = file.targets
  //     }
  //   }
  // }

  async loadYaml(file: string | null, filePath: string | null = null): any {
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
