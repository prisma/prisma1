import 'source-map-support/register'
import { Arg, Flags } from './Flags/index'
import { Output } from './Output'
import { Config } from './Config'
import { ProjectDefinition, RunOptions } from './types/common'
import { OutputArgs, OutputFlags, Parser } from './Parser'
import Help from './Help'
import { Client } from './Client/Client'
import { ProjectDefinitionClass } from './ProjectDefinition/ProjectDefinition'
import { Auth } from './Auth'
import { Environment } from './Environment'
import packagejson = require('../package.json')
import * as mock from './mock'
import * as fs from 'fs-extra'
import * as path from 'path'
import { RC } from './types/rc'
const debug = require('debug')('command')

const pjson = packagejson as any

export class Command {
  static topic: string
  static group: string
  static command?: string
  static description?: string
  static usage?: string
  static flags: Flags
  static args: Arg[] = []
  static aliases: string[] = []
  static hidden: boolean = false
  static mockDefinition: ProjectDefinition
  static mockRC: RC
  static allowAnyFlags: boolean = false

  static get id(): string {
    return this.command ? `${this.topic}:${this.command}` : this.topic
  }

  static async mock(...argv: any[]): Promise<Command> {
    let customArgs: any = null
    if (typeof argv[0] === 'object') {
      customArgs = argv.shift()
    }

    argv.unshift('argv0', 'cmd')

    const mockDefinition =
      customArgs && customArgs.mockDefinition
        ? customArgs.mockDefinition
        : mock.mockDefinition
    const mockRC = customArgs && customArgs.mockRC ? customArgs.mockRC : null
    const mockConfig = customArgs && customArgs.mockConfig ? customArgs.mockConfig : null
    debug(`Using mockDefinition`, mockDefinition)
    debug(`Using mockRC`, mockRC)

    return this.run({ argv, mock: true, mockDefinition, mockRC, mockConfig })
  }

  static async run(config?: RunOptions): Promise<Command> {
    if (process.env.NOCK_WRITE_RESPONSE_CMD === 'true') {
      debug('RECORDING')
      require('nock').recorder.rec({
        dont_print: true,
      })
    }
    const cmd = new this({ config })

    try {
      await cmd.init(config)
      await cmd.run()
      await cmd.out.done()
    } catch (err) {
      cmd.out.error(err)
    }

    if (process.env.NOCK_WRITE_RESPONSE_CMD === 'true') {
      const requests = require('nock').recorder.play()
      const requestsPath = path.join(process.cwd(), 'requests.js')
      debug('WRITING', requestsPath)
      fs.writeFileSync(requestsPath, requests.join('\n'))
    }
    return cmd
  }

  static buildHelp(config: Config): string {
    const help = new Help(config)
    return help.command(this)
  }

  static buildHelpLine(config: Config): string[] {
    const help = new Help(config)
    return help.commandLine(this)
  }

  protected static version = pjson.version

  client: Client
  out: Output
  config: Config
  definition: ProjectDefinitionClass
  auth: Auth
  env: Environment
  flags: OutputFlags
  args?: OutputArgs
  argv: string[]

  constructor(options: { config?: RunOptions } = { config: { mock: true } }) {
    this.config = options.config && options.config.mockConfig || options.config instanceof Config ? (options.config as any) : new Config(options.config)
    this.out = new Output(this.config)
    this.config.setOutput(this.out)
    this.argv = options.config && options.config.argv ? options.config.argv : []
    this.definition = new ProjectDefinitionClass(this.out, this.config)
    this.env = new Environment(this.out, this.config)
    this.client = new Client(this.config, this.env, this.out)
    this.auth = new Auth(this.out, this.config, this.env, this.client)
    this.client.setAuth(this.auth)
  }

  async run(...rest: void[]): Promise<void> {
    // noop
  }

  async init(options?: RunOptions) {
    // parse stuff here
    const mockDefinition = options && options.mockDefinition
    const mockRC = options && options.mockRC
    if (mockDefinition) {
      this.definition.set(mockDefinition)
    }
    if (mockRC) {
      this.env.localRC = mockRC
    }
    const parser = new Parser({
      flags: (this.constructor as any).flags || {},
      args: (this.constructor as any).args || [],
      variableArgs: (this.constructor as any).variableArgs,
      cmd: this,
    })
    if (this.argv && (this.argv.includes('-e') || this.argv.includes('--env'))) {
      this.out.warn('The usage of -e or --env is deprecated. Please use --target instead.')
    }
    const { argv, flags, args } = await parser.parse({
      flags: this.flags,
      argv: this.argv.slice(2),
    })
    this.flags = flags!
    this.argv = argv!
    this.args = args
    await this.env.load(flags!)
  }

  get stdout(): string {
    return this.out.stdout.output
  }

  get stderr(): string {
    return this.out.stderr.output
  }
}
