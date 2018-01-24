import { Output, Config } from 'prisma-cli-engine'
import { Environment, Cluster } from 'prisma-yml'
import * as childProcess from 'child_process'
import * as path from 'path'
import * as fs from 'fs-extra'
import chalk from 'chalk'
import { mapValues } from 'lodash'
import { getProcessForPort, printProcess } from './getProcessForPort'
import { getBinPath } from '../deploy/getbin'
import * as semver from 'semver'
import { ContainerInfo } from './types'
import { spawn } from '../../spawn'
import { userInfo } from 'os'
import * as Raven from 'raven'
const debug = require('debug')('Docker')
import * as portfinder from 'portfinder'
import { prettyTime } from '../../util'
import { createRsaKeyPair } from '../../utils/crypto'
import { defaultPort, defaultDBPort } from './constants'

export default class Docker {
  out: Output
  env: Environment
  config: Config
  cluster?: Cluster
  ymlPath: string = path.join(__dirname, 'docker/docker-compose.yml')
  frameworkYmlPath: string = path.join(
    __dirname,
    'docker/framework-docker-compose.yml',
  )
  envPath: string = path.join(__dirname, 'docker/env')
  frameworkEnvPath: string = path.join(__dirname, 'docker/oldenv')
  envVars: { [varName: string]: string }
  clusterName: string
  privateKey?: string
  stdout?: string
  psResult?: string
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
    let endpoint
    if (this.cluster) {
      endpoint = this.cluster.getDeployEndpoint()
      const sliced = endpoint.slice(endpoint.lastIndexOf(':') + 1)
      port = sliced.slice(0, sliced.indexOf('/'))
    }
    const defaultVars = this.getDockerEnvVars()
    if (!port) {
      port = await portfinder.getPortPromise({ port: defaultPort })
      if (port > defaultPort) {
        const useHigherPort = await this.askForHigherPort(
          String(defaultPort),
          port,
        )
        if (!useHigherPort) {
          port = defaultPort
        }
      }
      endpoint = `http://${this.hostName}:${port}`
    }
    await this.setEnvVars(port, endpoint, String(defaultDBPort))
    await this.ps()
    this.psResult = this.stdout

    // check if the db port (3307) is free
    const nextDBPort = await portfinder.getPortPromise({ port: defaultDBPort })
    if (nextDBPort > defaultDBPort) {
      if (!this.psResult!.includes(`:${defaultDBPort}->`)) {
        await this.askIfDBPortShouldBeUnblocked(String(defaultDBPort))
      }
    }
  }

  async setKeyPair() {
    const pair = await createRsaKeyPair()
    if (!this.envVars) {
      this.envVars = {}
    }
    this.envVars.CLUSTER_PUBLIC_KEY = pair.public
    this.privateKey = pair.private
    debug(pair)
  }

  saveCluster(): Cluster {
    const cluster = new Cluster(
      this.clusterName,
      `http://${this.hostName}:${this.envVars.PORT}`,
      this.privateKey,
    )
    debug('Saving cluster', cluster)
    this.env.addCluster(cluster)
    this.env.saveGlobalRC()
    return cluster
  }

  /**
   * returns true, if higher port should be used
   * @param port original port
   * @param higherPort alternative port
   */
  async askIfDBPortShouldBeUnblocked(port: string): Promise<void> {
    const processForPort = getProcessForPort(port)
    if (
      processForPort &&
      processForPort.command &&
      processForPort.command.includes('docker')
    ) {
      this.out.action.pause()
      const question = {
        name: 'confirmation',
        type: 'list',
        message: `Port ${port}, that is needed for the DB to be locally accessible, is already in use by ${printProcess(
          processForPort,
        )}. What do you want to do?`,
        choices: [
          {
            value: 'stop',
            name: `Stop container running on ${port}`,
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
        await this.stopContainersBlockingPort(port, true, true)
        this.out.action.resume()
        return
      }
      if (confirmation === 'next') {
        this.out.action.resume()
        return
      }
      if (confirmation === 'cancel') {
        this.out.exit(0)
      }
    } else {
      const processName =
        processForPort && processForPort.command
          ? ` (${printProcess(processForPort)})`
          : ''
      const instruction =
        processForPort && processForPort.processId
          ? `You can kill the process by running ${chalk.bold.green(
              `kill ${processForPort!.processId}`,
            )}`
          : `You can find the process id by running ${chalk.bold.green(
              `lsof -i:${port} -P -t -sTCP:LISTEN`,
            )}`

      throw new Error(`Port ${port}, which is used for binding the database, is already in use by a process${processName} that could not be stopped by docker-compose.
Please close the process by hand. ${instruction}`)
    }
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
            name: `Stop container running on ${port}`,
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
    if (this.psResult) {
      this.stdout = this.psResult
      return this
    }
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

  async kill(): Promise<Docker> {
    return this.run('kill')
  }

  async down(): Promise<Docker> {
    return this.run('down', '--remove-orphans', '-v', '--rmi', 'local')
  }

  async logs(): Promise<Docker> {
    await this.init()
    return this.run('logs')
  }

  async nuke(): Promise<Docker> {
    let port
    // if there is a cluster, try to stop it regularly
    if (this.cluster) {
      const endpoint = this.cluster.getDeployEndpoint()
      const sliced = endpoint.slice(endpoint.lastIndexOf(':') + 1)
      port = sliced.slice(0, sliced.indexOf('/'))
      await this.setEnvVars(port, endpoint, String(defaultDBPort))
      const before = Date.now()
      this.out.action.start('Nuking local cluster')
      await this.kill()
      await this.down()
      this.out.action.stop(prettyTime(Date.now() - before))
    }

    // if there is still a cluster on defaultPort or just a cluster we don't know, NUKE IT
    const used = getProcessForPort(defaultPort)
    if (used) {
      this.stopContainersBlockingPort(String(defaultPort), true)
    }
    const before = Date.now()
    this.out.action.start('Booting fresh local development cluster')
    await this.setEnvVars(
      String(defaultPort),
      `http://${this.hostName}:${defaultPort}`,
      String(defaultDBPort),
    )
    await this.run('up', '-d', '--remove-orphans')
    this.out.action.stop(prettyTime(Date.now() - before))
    return this
  }

  getDockerEnvVars(db: boolean = true) {
    const file = fs.readFileSync(
      db ? this.envPath : this.frameworkEnvPath,
      'utf-8',
    )
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

  async setEnvVars(port: string, endpoint: string, dbPort: string) {
    const defaultVars = this.getDockerEnvVars()
    const customVars = {
      PORT: port,
      SCHEMA_MANAGER_ENDPOINT: `http://prisma-database:${port}/cluster/schema`,
      CLUSTER_ADDRESS: `http://${this.hostName}:${port}`,
      DB_PORT: dbPort,
    }
    this.envVars = { ...process.env, ...defaultVars, ...customVars }
    await this.setKeyPair()
    debug(this.envVars)
  }

  async stopContainersBlockingPort(
    port: string,
    nuke: boolean = false,
    kill: boolean = false,
  ) {
    const output = childProcess.execSync('docker ps').toString()
    const containers = this.parsePs(output)
    const regex = /\d+\.\d+\.\d+\.\d+:(\d+)->/
    const blockingContainer = containers.find(c => {
      const match = c.PORTS.match(regex)
      return (match && match[1] === port) || false
    })

    if (blockingContainer) {
      const nameStart = blockingContainer.NAMES.split('_')[0]

      let error
      if (!kill) {
        try {
          if (nameStart.startsWith('localdatabase')) {
            this.setEnvVars(
              port,
              `http://${this.hostName}:${port}`,
              String(defaultDBPort),
            )
            await this.nukeContainers(nameStart, true)
          } else if (nameStart.startsWith('local')) {
            const defaultVars = this.getDockerEnvVars(false)
            const FUNCTIONS_PORT = '60050'
            const customVars = {
              PORT: String(port),
              FUNCTIONS_PORT,
              FUNCTION_ENDPOINT_INTERNAL: `http://localfaas:${FUNCTIONS_PORT}`,
              FUNCTION_ENDPOINT_EXTERNAL: `http://${
                this.hostName
              }:${FUNCTIONS_PORT}`,
              CLUSTER_ADDRESS: `http://${this.hostName}:${port}`,
            }
            this.envVars = { ...process.env, ...defaultVars, ...customVars }
            await this.nukeContainers(nameStart, false)
          }
        } catch (e) {
          error = e
        }
      }

      if (error || kill) {
        const relatedContainers = containers.filter(
          c => c.NAMES.split('_')[0] === nameStart,
        )
        const containerNames = relatedContainers.map(c => c.NAMES)
        await spawn('docker', [nuke ? 'kill' : 'stop'].concat(containerNames))
      }
      this.out.log('')
    } else {
      throw new Error(
        `Could not find container blocking port ${port}. Please stop it by hand using ${chalk.bold.green(
          'docker kill',
        )}`,
      )
    }
  }

  async nukeContainers(name: string, isDB: boolean = false) {
    await this.kill()
    await this.down()
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
    const bin = await this.getBin()
    const defaultArgs = [
      '-p',
      JSON.stringify(this.clusterName),
      '--file',
      this.ymlPath,
      '--project-directory',
      this.config.cwd,
    ]
    const args = defaultArgs.concat(argv)
    // this.out.log(chalk.dim(`$ docker-compose ${argv.join(' ')}\n`))
    debug({ bin, args })
    const output = await spawn(bin, args, {
      env: this.envVars,
      cwd: this.config.cwd,
    })
    debug(output)
    this.stdout = output
    return this
  }

  private async getBin(): Promise<string> {
    const bin = getBinPath('docker-compose')
    if (!bin) {
      throw new Error(
        `Please install docker-compose in order to run "prisma local".\nLearn more here: https://docs.docker.com/compose/install/`,
      )
    }

    let output
    try {
      output = childProcess.execSync('docker-compose -v').toString()
    } catch (e) {
      throw new Error(
        `Please install docker-compose 1.13.0 or greater in order to run "prisma local".\nLearn more here: https://docs.docker.com/compose/install/`,
      )
    }
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

    return bin as any
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
