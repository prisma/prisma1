import { RunOptions } from './types/common'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as cuid from 'scuid'
import * as findUp from 'find-up'
import { Output } from './Output/index'
import * as yaml from 'js-yaml'
const debug = require('debug')('config')


export class Config {
  /**
   * Local settings
   */
  out: Output
  debug: boolean = Boolean(
    process.env.DEBUG && process.env.DEBUG!.includes('*'),
  )
  windows: boolean = false
  bin: string = 'graphcool'
  mock: boolean = true
  argv: string[] = process.argv.slice(1)
  commandsDir: string = path.join(__dirname, '../dist/commands')
  defaultCommand: string = 'help'
  userPlugins: boolean = false
  version: string = '1.3.11'
  name: string = 'graphcool'
  pjson: any = {
    name: 'cli-engine',
    version: '0.0.0',
    dependencies: {},
    'cli-engine': {
      defaultCommand: 'help',
    },
  }
  sharedClusters: string[] = ['shared-eu-west-1', 'shared-ap-northeast-1', 'shared-us-west-2']

  /**
   * Paths
   */
  cwd: string
  home: string
  root = path.join(__dirname, '..')

  definitionDir: string
  definitionPath: string | null
  localRCPath: string
  globalRCPath: string
  warnings: string[] = []

  /**
   * Urls
   */
  authUIEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.console.graph.cool/cli/auth'
    : 'https://console.graph.cool/cli/auth'
  backendAddr = process.env.ENV === 'DEV'
    ? 'https://dev.api.graph.cool'
    : 'https://api.graph.cool'
  systemAPIEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.api.graph.cool/system'
    : 'https://api.graph.cool/system'
  authEndpoint = process.env.ENV === 'DEV'
    ? 'https://cli-auth-api.graph.cool/dev'
    : 'https://cli-auth-api.graph.cool/prod'
  docsEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.graph.cool/docs'
    : 'https://www.graph.cool/docs'
  statusEndpoint = 'https://crm.graph.cool/prod/status'

  /**
   * consumer endpoints
   */
  simpleAPIEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.api.graph.cool/simple/v1/'
    : 'https://api.graph.cool/simple/v1/'
  relayAPIEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.api.graph.cool/relay/v1/'
    : 'https://api.graph.cool/relay/v1/'
  fileAPIEndpoint = process.env.ENV === 'DEV'
    ? 'https://dev.api.graph.cool/file/v1/'
    : 'https://api.graph.cool/file/v1/'
  subscriptionsEndpoint = process.env.ENV === 'DEV'
    ? 'wss://dev.subscriptions.graph.cool'
    : 'wss://subscriptions.graph.cool'


  /* tslint:disable-next-line */
  __cache = {}

  constructor(options?: RunOptions) {
    this.cwd = (options && options.cwd) || this.getCwd()
    this.home = (options && options.home) || this.getHome()
    debug(`CWD`, this.cwd)
    debug(`HOME`, this.home)
    this.setDefinitionPaths()
    this.setRCPaths()
    if (options) {
      this.readPackageJson(options)
    }
  }
  setLocal(host: string = 'http://localhost:60000') {
    this.backendAddr = host
    this.systemAPIEndpoint = host + '/system'
  }
  setOutput(out: Output) {
    this.out = out
    this.warnings.forEach(warning  => out.warn(warning))
    this.warnings = []
  }
  get arch(): string {
    return os.arch() === 'ia32' ? 'x86' : os.arch()
  }
  get platform(): string {
    return os.platform() === 'win32' ? 'windows' : os.platform()
  }
  get userAgent(): string {
    return `${this.name}/${this.version} (${this.platform}-${this
      .arch}) node-${process.version}`
  }
  get dirname() {
    return this.pjson['cli-engine'].dirname || this.bin
  }
  get cacheDir() {
    const x = dir(
      this,
      'cache',
      this.platform === 'darwin'
        ? path.join(this.home, 'Library', 'Caches')
        : null,
    )
    return x
  }
  get requireCachePath() {
    return path.join(this.cacheDir, '/.require-cache.json')
  }
  private readPackageJson(options: RunOptions) {
    this.mock = options.mock
    this.argv = options.argv || this.argv
    if (options.root) {
      this.root = options.root
      const pjsonPath = path.join(options.root, 'package.json')
      const pjson = fs.readJSONSync(pjsonPath)
      if (pjson && pjson['cli-engine']) {
        this.pjson = pjson
        this.version = pjson.version
      }
    }
  }
  private setRCPaths() {
    this.localRCPath = path.join(this.definitionDir, '.graphcoolrc')
    const homePath = path.join(this.home, '.graphcoolrc')
    debug(`homepath`, homePath)
    this.globalRCPath = homePath
    debug(`localRCPath`, this.localRCPath)
    debug(`globalRCPath`, this.globalRCPath)
  }
  private warn(msg: string) {
    this.warnings.push(msg)
  }
  private setDefinitionPaths() {
    const definitionPath = path.join(this.cwd, 'graphcool.yml')
    if (fs.pathExistsSync(definitionPath)) {
      this.definitionDir = this.cwd
      this.definitionPath = definitionPath
    } else {
      const found = findUp.sync('graphcool.yml', {cwd: this.cwd})
      this.definitionDir = found ? path.dirname(found) : this.cwd
      this.definitionPath = found || null
    }
    debug(`definitionDir`, this.definitionDir)
    debug(`definitionPath`, this.definitionPath)
  }
  private getCwd() {
    // get cwd
    let cwd = process.cwd()
    if (process.env.NODE_ENV === 'test') {
      cwd = path.join(os.tmpdir(), `${cuid()}/`)
      fs.mkdirpSync(cwd)
      debug('cwd', cwd)
    }
    return cwd
  }
  private getHome() {
    // get home
    let home = os.homedir() || os.tmpdir()
    if (process.env.NODE_ENV === 'test') {
      home = path.join(os.tmpdir(), `${cuid()}/`)
      fs.mkdirpSync(home)
      debug('home', home)
    }
    return home
  }
}

function dir(config: Config, category: string, d: string | null): string {
  const cacheKey = `dir:${category}`
  const cache = config.__cache[cacheKey]
  if (cache) {
    return cache
  }
  d =
    d ||
    path.join(
      config.home,
      category === 'data' ? '.local/share' : '.' + category,
    )
  if (config.windows) {
    d = process.env.LOCALAPPDATA || d
  }
  d = process.env.XDG_DATA_HOME || d
  d = path.join(d, config.dirname)
  fs.mkdirpSync(d)
  config.__cache[cacheKey] = d
  return d
}
