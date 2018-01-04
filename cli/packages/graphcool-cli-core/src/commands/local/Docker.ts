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
import { ContainerInfo } from './types'
import { spawn } from '../../spawn'
import { userInfo } from 'os'
import * as Raven from 'raven'
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
        const useHigherPort = await this.askForHigherPort('60000', port)
        if (!useHigherPort) {
          port = 60000
        }
      }
    }
    const customVars = {
      PORT: String(port),
      SCHEMA_MANAGER_ENDPOINT: `http://graphcool-database:${port}/cluster/schema`,
    }
    debug(`customVars`)
    debug(customVars)
    // this.out.log(
    //   `Running local Graphcool cluster at http://localhost:${customVars.PORT}`,
    // )
    // this.out.log(`This may take several minutes`)
    this.envVars = { ...process.env, ...defaultVars, ...customVars }
  }
  /**
   * returns true, if higher port should be used
   * @param port original port
   * @param higherPort alternative port
   */
  async askForHigherPort(port: string, higherPort: string): Promise<boolean> {
    const processForPort = getProcessForPort(port)
    if (processForPort) {
      const question = {
        name: 'confirmation',
        type: 'list',
        message: `Port ${port} is already in use by ${processForPort}. What do you want to do?`,
        choices: [
          {
            value: 'stop',
            name: 'Stop container running on 60000',
          },
          {
            value: 'next',
            name: `Use port ${higherPort}`,
          },
          {
            value: 'cancel',
            name: 'Do not continue',
          },
        ],
        pageSize: 8,
      }
      const { confirmation }: { confirmation: string } = await this.out.prompt(
        question,
      )
      if (confirmation === 'stop') {
        await this.stopContainersBlockingPort(port)
        return false
      }
      if (confirmation === 'next') {
        return true
      }
      if (confirmation === 'cancel') {
        this.out.exit(0)
      }
    }

    await Raven.captureException(
      new Error('Port is blocked, but could not find blocking process'),
    )
    return true
  }

  async up(): Promise<Docker> {
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

  async stopContainersBlockingPort(port: string) {
    const output = childProcess.execSync('docker ps').toString()
    const containers = this.parsePs(output)
    const regex = /\d+\.\d+\.\d+\.\d+:(\d+)->/
    const blockingContainer = containers.find(c => {
      const match = c.PORTS.match(regex)
      return (match && match[1] === port) || false
    })
    if (blockingContainer) {
      const nameStart = blockingContainer.NAMES.split('_')[0]
      const relatedContainers = containers.filter(
        c => c.NAMES.split('_')[0] === nameStart,
      )
      const containerNames = relatedContainers.map(c => c.NAMES)
      this.out.action.start(`Stopping docker containers`)
      await spawn('docker', ['stop'].concat(containerNames))
      this.out.action.stop()
      this.out.log('')
    } else {
      throw new Error(
        `Could not find container blocking port ${port}. Please either stop it by hand or choose another port.`,
      )
    }
  }

  private parsePs(output): ContainerInfo[] {
    if (!output) {
      return []
    }

    const lines = output.trim().split('\n')

    if (lines.length < 2) {
      return []
    }

    const headers = {}
    const start = 0
    lines[0].replace(/([A-Z\s]+?)($|\s{2,})/g, (all, name, space, index) => {
      headers[name] = {
        start: index,
        end: index + all.length,
      }

      // check if this header is at the end of the line
      if (space.length === 0) {
        headers[name].end = undefined
      }
      return name + ' '
    })

    const entries: any = []
    for (let i = 1; i < lines.length; i++) {
      const entry = {}
      for (const key in headers) {
        if (headers.hasOwnProperty(key)) {
          entry[key] = lines[i]
            .substring(headers[key].start, headers[key].end)
            .trim()
        }
      }
      entries.push(entry)
    }

    return entries
  }

  private async run(...argv: string[]): Promise<Docker> {
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
    // this.out.log(chalk.dim(`$ docker-compose ${argv.join(' ')}\n`))
    await spawn('docker-compose', args, {
      env: this.envVars,
      cwd: this.config.cwd,
    })
    return this
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
