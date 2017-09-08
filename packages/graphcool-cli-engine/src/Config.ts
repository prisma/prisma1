import { RunOptions } from './types'
import * as path from 'path'
import * as os from 'os'
import * as fs from 'fs-extra'

export class Config {
  debug: boolean = Boolean(process.env.DEBUG && process.env.DEBUG!.includes('*'))
  windows: boolean = false
  bin: string = 'graphcool'
  mock: boolean = true
  argv: string[] = process.argv.slice(1)
  // TODO check if this is correct
  commandsDir: string = path.join(__dirname, '../dist/commands')
  defaultCommand: string = 'help'
  userPlugins: boolean = false
  // TODO inject via package json of module.parent.filepath
  version: string = '1.4'
  name: string = 'graphcool'
  // TODO gather package json later
  pjson: any = {
    name: 'cli-engine',
    version: '0.0.0',
    dependencies: {},
    'cli-engine': {
      defaultCommand: 'help',
    }
  }
  graphcoolRCFilePath = path.join(os.homedir(), '.graphcool')
  // TODO put in root field
  root = path.join(__dirname, '..')
  /* tslint:disable-next-line */
  __cache = {}
  home = os.homedir() || os.tmpdir()
  token: string
  authUIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.console.graph.cool/cli/auth' : 'https://console.graph.cool/cli/auth'
  systemAPIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.api.graph.cool/system' : 'https://api.graph.cool/system'
  authEndpoint = process.env.ENV === 'DEV' ? 'https://cli-auth-api.graph.cool/dev' : 'https://cli-auth-api.graph.cool/prod'
  docsEndpoint = process.env.ENV === 'DEV' ? 'https://dev.graph.cool/docs' : 'https://www.graph.cool/docs'
  statusEndpoint = 'https://crm.graph.cool/prod/status'

  constructor(options?: RunOptions) {
    // noop
    if (fs.pathExistsSync(this.graphcoolRCFilePath)) {
      const configContent = fs.readFileSync(this.graphcoolRCFilePath, 'utf-8')
      this.token = JSON.parse(configContent).token
    }

    if (options) {
      this.mock = options.mock
      this.argv = options.argv || this.argv
      if (options.root) {
        this.root = options.root
        const pjsonPath = path.join(options.root, 'package.json')
        this.pjson = fs.readJSONSync(pjsonPath)
      }
    }
  }
  setToken(token: string) {
    this.token = token
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
