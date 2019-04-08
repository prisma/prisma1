import 'source-map-support/register'
import { Arg, Flags } from './Flags/index'
import { Output } from './Output'
import { Config } from './Config'
import { ProjectDefinition, RunOptions } from './types/common'
import { OutputArgs, OutputFlags, Parser } from './Parser'
import Help from './Help'
import { Client } from './Client/Client'
import { Environment, PrismaDefinitionClass } from 'prisma-yml'
import packagejson = require('../package.json')
import * as mock from './mock'
import { RC } from './types/rc'
import { initStatusChecker } from './StatusChecker'
import { filterObject } from './util'
import * as os from 'os'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk';
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
  static deprecated?: boolean = false
  static printVersionSyncWarning: boolean = false

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
    const mockConfig =
      customArgs && customArgs.mockConfig ? customArgs.mockConfig : null
    debug(`Using mockDefinition`, mockDefinition)
    debug(`Using mockRC`, mockRC)

    return this.run({ argv, mock: true, mockDefinition, mockRC, mockConfig })
  }

  printServerVersion(version: string | null) {
    if (version) {
      return `${os.EOL}Prisma server version: ${version}`
    } else {
      return ``
    }
  }

  printCLIVersion() {
    return `Prisma CLI version: ${this.config.userAgent}`
  }

  getMinorVersion(v) {
    const tokens = v.split('.')
    if (tokens.length < 2) {
      throw new Error(`Unable to construct minor version from ${v}`)
    }
    return `${tokens[0]}.${tokens[1]}`
  }

  printVersionSyncWarningMessage() {
    return `${chalk.yellow(chalk.bold(`Warning: Your Prisma server and Prisma CLI are currently out of sync. They should be on the same minor version.`))}`
  }

  compareVersions(v1, v2) {
    return this.getMinorVersion(v1) === this.getMinorVersion(v2)
  }

  async areServerAndCLIInSync(cmd: Command): Promise<{
    inSync: boolean,
    serverVersion: string | null
  }> {
    try {
      const envFile = cmd.flags['env-file']
      if (envFile && !fs.pathExistsSync(path.join(cmd.config.cwd, envFile))) {
        await cmd.out.error(`--env-file path '${envFile}' does not exist`)
      }
      await cmd.definition.load(cmd.flags, envFile)

      const server = await cmd.definition.getCluster(false)
      if (server) {
        try {
          const serverVersion = await server.getVersion()
          return {
            inSync: this.compareVersions(serverVersion, cmd.config.version),
            serverVersion
          }
        } catch (e) {
          debug(`Failed to fetch server version`)
          debug(e.toString())
          return {
            inSync: true,
            serverVersion: null
          }
        }
      } else {
        debug(`Failed to get the server`)
        return {
          inSync: true,
          serverVersion: null
        }
      }
    } catch(e) {
      debug(`Failed to get the definition file`)
      debug(e.toString())
      return {
        inSync: true,
        serverVersion: null
      }
    }
  }

  static async run(config?: RunOptions): Promise<Command> {
    const cmd = new this({ config })

    try {
      await cmd.init(config)
      
      if (this.printVersionSyncWarning) {
        const { inSync, serverVersion } = await cmd.areServerAndCLIInSync(cmd)
        if (!inSync) {
          cmd.out.log(`${cmd.printVersionSyncWarningMessage()}
  
  ${cmd.printCLIVersion()}${cmd.printServerVersion(serverVersion)}
  
  For further information, please read: http://bit.ly/prisma-cli-server-sync
  `)
        }
      }

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
  definition: PrismaDefinitionClass
  // auth: Auth
  env: Environment
  flags: OutputFlags
  args?: OutputArgs
  argv: string[]

  constructor(options: { config?: RunOptions } = { config: { mock: false } }) {
    if (options.config && options.config instanceof Config) {
      this.config = options.config
    } else if (options && options.config && options.config.mockConfig) {
      this.config = options.config.mockConfig
    } else {
      this.config = new Config(options.config)
    }
    this.out = new Output(this.config)
    this.config.setOutput(this.out)
    this.argv = options.config && options.config.argv ? options.config.argv : []
    this.env = new Environment(this.config.home, this.out, this.config.version)
    this.definition = new PrismaDefinitionClass(
      this.env,
      this.config.definitionPath,
      process.env,
      this.out,
    )
    this.client = new Client(this.config, this.env, this.out)
    // this.auth = new Auth(this.out, this.config, this.env, this.client)
    // this.client.setAuth(this.auth)
  }

  async run(...rest: void[]): Promise<void> {
    // noop
  }

  async init(options?: RunOptions) {
    const parser = new Parser({
      flags: (this.constructor as any).flags || {},
      args: (this.constructor as any).args || [],
      variableArgs: (this.constructor as any).variableArgs,
      cmd: this,
    })
    const { argv, flags, args } = await parser.parse({
      flags: this.flags,
      argv: this.argv.slice(2),
    })
    this.flags = flags!
    this.argv = argv!
    this.args = args
    await this.env.load()
    initStatusChecker(this.config, this.env)
  }

  get stdout(): string {
    return this.out.stdout.output
  }

  get stderr(): string {
    return this.out.stderr.output
  }

  getSanitizedFlags(): OutputFlags {
    return filterObject(this.flags, (_, value) => {
      if (value === undefined) {
        return false
      }

      return true
    })
  }
}
