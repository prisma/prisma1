import { Output, Config, TargetDefinition } from 'graphcool-cli-engine'
import * as childProcess from 'child_process'
import * as path from 'path'
import * as fs from 'fs-extra'
import * as chalk from 'chalk'
import * as portfinder from 'portfinder'

export default class Docker {
  out: Output
  config: Config
  projectName: string
  ymlPath: string = path.join(__dirname, 'docker/docker-compose.yml')
  envPath: string = path.join(__dirname, 'docker/.envrc')
  envVars: {[varName: string]: string}
  target?: TargetDefinition
  constructor(out: Output, config: Config, projectName: string) {
    this.out = out
    this.config = config
    this.projectName = projectName
    this.target = this.config.targets[projectName]
  }

  async init() {
    // either get the ports
    const defaultVars = this.getDockerEnvVars()
    const port = (this.target && this.target.host) ? this.target.host.split(':').slice(-1)[0] : undefined
    const customVars = {
      PORT: String(port || await portfinder.getPortPromise({port: 60000})),
    }
    this.out.log(`Using http://localhost:${customVars.PORT} as the local Graphcool host`)
    this.envVars = {...process.env, ...defaultVars, ...customVars}
  }

  async up(): Promise<Docker> {
    await this.init()
    return this.run('up', '-d', '--remove-orphans')
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
    return this.run('pull', '--parallel')
  }

  private run(...argv: string[]): Promise<Docker> {
    return new Promise((resolve, reject) => {
      const defaultArgs = ['-p', JSON.stringify(this.projectName), '--file', this.ymlPath, '--project-directory', this.config.cwd]
      const args = defaultArgs.concat(argv)
      this.out.log(chalk.dim(`$ docker-compose ${args.join(' ')}\n`))
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

  private format(data: string | Buffer) {
    return data.toString().trim().split('\n').map(l => `${chalk.blue('docker')}   ${l}`).join('\n')
  }

  private getDockerEnvVars() {
    const file = fs.readFileSync(this.envPath, 'utf-8')
    return this.parseEnv(file)
  }

  private parseEnv(src: string) {
    const regex = /^\s*export\s*([a-zA-Z0-9\.\-_]+)\s*=(.*)?\s*/
    return src
      .toString()
      .split('\n')
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

        return {...acc, [key]: value}
      }, {})
  }
}
