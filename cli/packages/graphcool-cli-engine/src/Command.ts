import 'source-map-support/register'
import { Arg, Flags } from './Flags/index'
import { Output } from './Output'
import { Config } from './Config'
import { RunOptions } from './types'
import { OutputArgs, OutputFlags, Parser } from './Parser'
import Help from './Help'
import { Client } from './Client/Client'
import { ProjectDefinitionClass } from './ProjectDefinition/ProjectDefinition'
import { Auth } from './Auth'
import { Environment } from './Environment'
import packagejson = require('../package.json')

const pjson = packagejson as any

export class Command {
  static topic: string
  static command?: string
  static description?: string
  static usage?: string
  static flags: Flags
  static args: Arg[] = []
  static aliases: string[] = []
  static hidden: boolean = false

  static get id(): string {
    return this.command ? `${this.topic}:${this.command}` : this.topic
  }

  static async mock(...argv: string[]): Promise<Command> {
    argv.unshift('argv0', 'cmd')
    return this.run({argv, mock: true})
  }

  static async run(config?: RunOptions): Promise<Command> {
    const cmd = new this({config})
    try {
      await cmd.init()
      await cmd.run()
      await cmd.out.done()
    } catch (err) {
      cmd.out.error(err)
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

  constructor(options: { config?: RunOptions } = {config: {mock: true}}) {
    this.config = new Config(options.config)
    this.out = new Output(this.config)
    this.argv = (options.config && options.config.argv) ? options.config.argv : []
    this.definition = new ProjectDefinitionClass(this.out, this.config)
    this.client = new Client(this.config)
    this.auth = new Auth(this.out, this.config, this.client)
    this.env = new Environment(this.out, this.config, this.client)
    this.env.load()
  }

  async run(...rest: void[]): Promise<void> {
    // noop
  }

  async init() {
    // parse stuff here
    const parser = new Parser({
      flags: (this.constructor as any).flags || {},
      args: (this.constructor as any).args || [],
      variableArgs: (this.constructor as any).variableArgs,
      cmd: this
    })
    const {argv, flags, args} = await parser.parse({flags: this.flags, argv: this.argv.slice(2)})
    this.flags = flags!
    this.argv = argv!
    this.args = args
  }

  get stdout(): string {
    return this.out.stdout.output
  }

  get stderr(): string {
    return this.out.stderr.output
  }
}