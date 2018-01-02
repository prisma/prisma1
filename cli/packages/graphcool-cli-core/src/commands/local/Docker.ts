import { Output, Config } from 'graphcool-cli-engine'
import { Environment, Cluster } from 'graphcool-yml'
import * as childProcess from 'child_process'
import * as path from 'path'
import * as fs from 'fs-extra'
import chalk from 'chalk'
import { mapValues } from 'lodash'
import { getProcessForPort } from './getProcessForPort'
import { getBinPath } from '../deploy/getbin'
import * as semver from 'semver'
const debug = require('debug')('Docker')

export default class Docker {
  out: Output
  env: Environment
  config: Config
  cluster?: Cluster
  ymlPath: string = path.join(__dirname, 'docker/docker-compose.yml')
  envPath: string = path.join(__dirname, 'docker/env')
  envVars: { [varName: string]: string }
  clusterName: string
  constructor(
    out: Output,
    config: Config,
    env: Environment,
    clusterName: string,
  ) {
    this.out = out
    this.config = config
    this.env = env
    this.cluster = env.clusterByName(clusterName)
    this.clusterName = clusterName
    if (this.cluster) {
      env.setActiveCluster(this.cluster)
    }
  }

  get hostName(): string {
    if (process.env.GRAPHCOOL_HOST) {
      return process.env.GRAPHCOOL_HOST!
    }
    if (process.env.DOCKER_HOST) {
      const ipRegex = /(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})/
      const match = ipRegex.exec(process.env.DOCKER_HOST!)
      if (match) {
        return match[1]
      }
    }

    return 'localhost'
  }
  async init() {
    // either get the ports
    let port: any = null
    if (this.cluster) {
      const endpoint = this.cluster.getDeployEndpoint()
      const sliced = endpoint.slice(endpoint.lastIndexOf(':') + 1)
      port = sliced.slice(0, sliced.indexOf('/'))
    }
    const defaultVars = this.getDockerEnvVars()
    const portfinder = require('portfinder')
    if (!port) {
      port = await portfinder.getPortPromise({ port: 60000 })
      if (port > 60000) {
        await this.askForHigherPort('60000', port)
      }
    }
    const customVars = {
      PORT: String(port),
      SCHEMA_MANAGER_ENDPOINT: `http://graphcool-database:${port}/cluster/schema`,
    }
    debug(`customVars`)
    debug(customVars)
    this.out.log(
      `Running local Graphcool cluster at http://localhost:${customVars.PORT}`,
    )
    this.out.log(`This may take several minutes`)
    this.envVars = { ...process.env, ...defaultVars, ...customVars }
  }
  async askForHigherPort(port: string, higherPort: string) {
    const processForPort = getProcessForPort(port)
    if (processForPort) {
      const confirmationQuestion = {
        name: 'confirmation',
        type: 'input',
        message: `Port 60000 is already used by ${processForPort}. Do you want to use the next free port (${higherPort})? (n/Y)`,
        default: 'Y',
      }
      const { confirmation }: { confirmation: string } = await this.out.prompt(
        confirmationQuestion,
      )
      if (confirmation.toLowerCase().startsWith('n')) {
        this.out.exit(0)
      }
    }
  }

  async up(): Promise<Docker> {
    await this.init()
    return this.run('up', '-d', '--remove-orphans')
  }

  async ps(): Promise<Docker> {
    await this.init()
    return this.run('ps')
  }

  async start(): Promise<Docker> {
    await this.init()
    return this.run('start')
  }

  async stop(): Promise<Docker> {
    await this.init()
    return this.run('stop')
  }

  async restart(): Promise<Docker> {
    await this.init()
    return this.run('restart')
  }

  async pull(): Promise<Docker> {
    await this.init()
    return this.run('pull')
  }

  async logs(): Promise<Docker> {
    await this.init()
    return this.run('logs', '-f')
  }

  async nuke(): Promise<Docker> {
    await this.init()
    await this.run('down', '--remove-orphans', '-v', '--rmi', 'local')
    return this.run('up', '-d', '--remove-orphans')
  }

  getDockerEnvVars() {
    const file = fs.readFileSync(this.envPath, 'utf-8')
    return this.parseEnv(file)
  }

  parseEnv(src: string) {
    const regex = /^\s*export\s*([a-zA-Z0-9\.\-_]+)\s*=(.*)?\s*/
    const variableSyntax = new RegExp(
      '\\${([ ~:a-zA-Z0-9._\'",\\-\\/\\(\\)]+?)}',
      'g',
    )
    const vars = src
      .toString()
      .split(/\r\n|\r|\n/g)
      .reduce((acc, line) => {
        if (line.trim().startsWith('#')) {
          return acc
        }
        const match = line.match(regex)
        if (!match) {
          return acc
        }

        const key = match[1]
        let value = match[2] || ''
        const length = value ? value.length : 0
        if (length > 0 && value.startsWith('"') && value.endsWith('"')) {
          value = value.replace(/\\n/gm, '\n')
        }
        value = value.replace(/(^['"]|['"]$)/g, '').trim()

        return { ...acc, [key]: value }
      }, {})

    return mapValues(vars, (value: string, key) => {
      const match = variableSyntax.exec(value)
      if (match) {
        const varName = match[1]
        if (vars[varName]) {
          const newValue =
            value.slice(0, match.index) +
            vars[varName] +
            value.slice(match.index + match[0].length)
          return newValue
        } else {
          this.out.warn(
            `No variable for env var ${key} and value ${match[0]} found`,
          )
        }
      }
      return value
    })
  }

  private run(...argv: string[]): Promise<Docker> {
    return new Promise(async (resolve, reject) => {
      await this.checkBin()
      const defaultArgs = [
        '-p',
        JSON.stringify(this.clusterName + '-database'),
        '--file',
        this.ymlPath,
        '--project-directory',
        this.config.cwd,
      ]
      const args = defaultArgs.concat(argv)
      this.out.log(chalk.dim(`$ docker-compose ${argv.join(' ')}\n`))
      const child = childProcess.spawn('docker-compose', args, {
        env: this.envVars,
        cwd: this.config.cwd,
      })
      child.stdout.on('data', data => {
        this.out.log(this.format(data))
      })
      child.stderr.on('data', data => {
        this.out.log(this.format(data))
      })
      child.on('error', err => {
        this.out.error(err)
      })
      child.on('close', code => {
        if (code !== 0) {
          reject(code)
        } else {
          resolve(this)
        }
      })
    })
  }

  private async checkBin() {
    const bin = getBinPath('docker-compose')
    if (!bin) {
      throw new Error(
        `Please install docker-compose in order to run "graphcool local".\nLearn more here: https://docs.docker.com/compose/install/`,
      )
    }

    const output = childProcess.execSync('docker-compose -v').toString()
    const regex = /.*?(\d{1,2}\.\d{1,2}\.\d{1,2}),?/
    const match = output.match(regex)
    if (match && match[1]) {
      const version = match[1]
      const greater = semver.gt(version, '1.13.0')
      if (!greater) {
        throw new Error(
          `Your docker-compose version ${version} is too old. Please update to the latest docker-compose https://github.com/docker/compose/releases`,
        )
      }
    }
  }

  private format(data: string | Buffer) {
    return data
      .toString()
      .trim()
      .split(/\n/)
      .map(l => `${chalk.blue('docker')}   ${l}`)
      .join('\n')
  }
}
