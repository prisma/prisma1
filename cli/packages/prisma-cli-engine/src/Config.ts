import { RunOptions } from './types/common'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as cuid from 'scuid'
import * as findUp from 'find-up'
import { Output } from './Output/index'
import * as yaml from 'js-yaml'
const debug = require('debug')('config')
import { getGraphQLConfig } from 'graphql-config'
import { values } from 'lodash'
import { getRoot } from './util'

const isDevConsole =
  (process.env.CONSOLE_ENDPOINT || '').toLowerCase() === 'dev'

export class Config {
  /**
   * Local settings
   */
  mockInquirer?: any
  out: Output
  debug: boolean = Boolean(
    process.env.DEBUG && process.env.DEBUG!.includes('*'),
  )
  windows: boolean = false
  bin: string = 'prisma'
  mock: boolean = true
  argv: string[] = process.argv.slice(1)
  commandsDir: string = path.join(__dirname, '../dist/commands')
  defaultCommand: string = 'help'
  userPlugins: boolean = false
  version: string = '1.1'
  name: string = 'prisma'
  pjson: any = {
    name: 'cli-engine',
    version: '0.0.0',
    dependencies: {},
    'cli-engine': {
      defaultCommand: 'help',
    },
  }

  /**
   * Paths
   */
  cwd: string
  home: string
  root = path.join(__dirname, '..')

  definitionDir: string
  definitionPath: string | null
  globalPrismaPath: string
  globalConfigPath: string
  globalClusterCachePath: string
  warnings: string[] = []

  /**
   * Urls
   */
  cloudApiEndpoint =
    process.env.CLOUD_API_ENDPOINT || 'https://api2.cloud.prisma.sh'
  consoleEndpoint = isDevConsole
    ? 'http://localhost:3000'
    : 'https://app.prisma.io'

  /* tslint:disable-next-line */
  __cache = {}

  constructor(options?: RunOptions) {
    this.cwd = (options && options.cwd) || this.getCwd()
    this.home = (options && options.home) || this.getHome()
    debug(`CWD`, this.cwd)
    debug(`HOME`, this.home)
    this.setDefinitionPaths()
    this.setPaths()
    this.readPackageJson(options!)
    if (options && options.mockInquirer) {
      this.mockInquirer = options.mockInquirer
    }
  }
  setOutput(out: Output) {
    this.out = out
    this.warnings.forEach(warning => out.warn(warning))
    this.warnings = []
  }
  get arch(): string {
    return os.arch() === 'ia32' ? 'x86' : os.arch()
  }
  get platform(): string {
    return os.platform() === 'win32' ? 'windows' : os.platform()
  }
  get userAgent(): string {
    return `${this.name}/${this.version} (${this.platform}-${this.arch}) node-${
      process.version
    }`
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
  get requestsCachePath() {
    return path.join(this.cacheDir, '/.requests.json')
  }
  findConfigDir(): null | string {
    const configPath = findUp.sync(
      ['.graphqlconfig', '.graphqlconfig.yml', '.graphqlconfig.yaml'],
      {
        cwd: this.cwd,
      },
    )

    if (configPath) {
      return path.dirname(configPath)
    }

    return null
  }
  private readPackageJson(options?: RunOptions) {
    if (options) {
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
    } else {
      const root = getRoot()
      this.root = root
      const pjsonPath = path.join(root, 'package.json')
      const pjson = fs.readJSONSync(pjsonPath)
      if (pjson && pjson['cli-engine']) {
        this.pjson = pjson
        this.version = pjson.version
      }
    }
  }
  private setPaths() {
    this.globalPrismaPath = path.join(this.home, '.prisma/')
    this.globalConfigPath = path.join(this.home, '.prisma/config.yml')
    this.globalClusterCachePath = path.join(this.home, '.prisma/cache.yml')
  }
  private warn(msg: string) {
    this.warnings.push(msg)
  }
  private setDefinitionPaths() {
    const definitionPath = path.join(this.cwd, 'prisma.yml')
    const definitionPathWithPrisma = path.join(this.cwd, 'prisma', 'prisma.yml')
    if (fs.pathExistsSync(definitionPath)) {
      this.definitionDir = this.cwd
      this.definitionPath = definitionPath
    } else if (fs.pathExistsSync(definitionPathWithPrisma)) {
      this.definitionDir = path.join(this.cwd, 'prisma')
      this.definitionPath = definitionPathWithPrisma
    } else {
      this.definitionPath = this.getDefinitionPathByGraphQLConfig()
      if (this.definitionPath) {
        this.definitionDir = path.dirname(this.definitionPath)
      } else {
        const found = findUp.sync('prisma.yml', { cwd: this.cwd })
        this.definitionDir = found ? path.dirname(found) : this.cwd
        this.definitionPath = found || null
      }
    }

    debug(`definitionDir`, this.definitionDir)
    debug(`definitionPath`, this.definitionPath)
  }
  private getDefinitionPathByGraphQLConfig(): string | null {
    // try to lookup with graphql config
    let definitionPath
    try {
      const configDir = this.findConfigDir()!
      const config = getGraphQLConfig(configDir).config

      const allExtensions = [
        config.extensions,
        ...values(config.projects).map(p => p.extensions),
      ]

      const prismaExtension = allExtensions.find(e => Boolean(e && e.prisma))
      if (prismaExtension) {
        let { prisma } = prismaExtension
        if (!fs.pathExistsSync(prisma)) {
          prisma = path.join(configDir, prisma)
        }
        definitionPath = path.resolve(prisma)
      }
      this.definitionDir = configDir
    } catch (e) {
      debug(e)
    }

    return definitionPath
  }
  private getCwd() {
    // get cwd
    let cwd = process.cwd()
    if (process.env.NODE_ENV === 'test' && process.env.TEST_PRISMA_CLI) {
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
