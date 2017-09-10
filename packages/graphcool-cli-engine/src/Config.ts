import { RunOptions } from './types'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as cuid from 'cuid'

// TODO replace with process.cwd()
let cwd = '/Users/tim/code/cli-tests/new'
if (process.env.NODE_ENV === 'test') {
  cwd = path.join(os.tmpdir(), `${cuid()}/`)
  fs.mkdirpSync(cwd)
}

let home = os.homedir() || os.tmpdir()

if (process.env.NODE_ENV === 'test') {
  home = path.join(os.tmpdir(), `${cuid()}/`)
  fs.mkdirpSync(home)
}

export class Config {
  /**
   * Local settings
   */
  debug: boolean = Boolean(process.env.DEBUG && process.env.DEBUG!.includes('*'))
  windows: boolean = false
  bin: string = 'graphcool'
  mock: boolean = true
  argv: string[] = process.argv.slice(1)
  commandsDir: string = path.join(__dirname, '../dist/commands')
  defaultCommand: string = 'help'
  userPlugins: boolean = false
  // TODO inject via package json of module.parent.filepath
  version: string = '1.4'
  name: string = 'graphcool'
  pjson: any = {
    name: 'cli-engine',
    version: '0.0.0',
    dependencies: {},
    'cli-engine': {
      defaultCommand: 'help',
    }
  }

  /**
   * Paths
   */
  root = path.join(__dirname, '..')
  envPath = path.join(cwd, '.graphcoolrc')
  definitionDir = cwd
  home = home
  dotGraphcoolFilePath = path.join(home, '.graphcool')

  /**
   * Urls
   */
  token: string | null
  authUIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.console.graph.cool/cli/auth' : 'https://console.graph.cool/cli/auth'
  backendAddr = process.env.ENV === 'DEV' ? 'https://dev.api.graph.cool' : 'https://api.graph.cool'
  systemAPIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.api.graph.cool/system' : 'https://api.graph.cool/system'
  authEndpoint = process.env.ENV === 'DEV' ? 'https://cli-auth-api.graph.cool/dev' : 'https://cli-auth-api.graph.cool/prod'
  docsEndpoint = process.env.ENV === 'DEV' ? 'https://dev.graph.cool/docs' : 'https://www.graph.cool/docs'
  statusEndpoint = 'https://crm.graph.cool/prod/status'

  /* tslint:disable-next-line */
  __cache = {}

  constructor(options?: RunOptions) {
    // noop
    if (process.env.NODE_ENV === 'test') {
      this.token = 'test token'
    } else {
      if (fs.pathExistsSync(this.dotGraphcoolFilePath)) {
        const configContent = fs.readFileSync(this.dotGraphcoolFilePath, 'utf-8')
        this.token = JSON.parse(configContent).token
      }
    }

    if (options) {
      this.mock = options.mock
      this.argv = options.argv || this.argv
      if (options.root) {
        this.root = options.root
        const pjsonPath = path.join(options.root, 'package.json')
        const pjson = fs.readJSONSync(pjsonPath)
        if (pjson && pjson['cli-engine']) {
          this.pjson = pjson
        }
      }
    }
  }
  setToken(token: string | null) {
    this.token = token
  }
  saveToken() {
    const json = JSON.stringify({token: this.token}, null, 2)
    fs.writeFileSync(this.dotGraphcoolFilePath, json)
  }
  get arch(): string {
    return os.arch() === 'ia32' ? 'x86' : os.arch()
  }
  get platform(): string {
    return os.platform() === 'win32' ? 'windows' : os.platform()
  }
  get userAgent(): string {
    return `${this.name}/${this.version} (${this.platform}-${this.arch}) node-${process.version}`
  }
  get dirname () {
    return this.pjson['cli-engine'].dirname || this.bin
  }
  get cacheDir () {
    return dir(this, 'cache', this.platform === 'darwin' ? path.join(this.home, 'Library', 'Caches') : null)
  }
}

function dir (config: Config, category: string, d: string | null): string {
  const cacheKey = `dir:${category}`
  const cache = config.__cache[cacheKey]
  if (cache) {
    return cache
  }
  d = d || path.join(config.home, category === 'data' ? '.local/share' : '.' + category)
  if (config.windows) {
    d = process.env.LOCALAPPDATA || d
  }
  d = process.env.XDG_DATA_HOME || d
  d = path.join(d, config.dirname)
  fs.mkdirpSync(d)
  config.__cache[cacheKey] = d
  return d
}
