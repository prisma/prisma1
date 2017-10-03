import { Output, Config } from 'graphcool-cli-engine'
import * as childProcess from 'child_process'
import * as path from 'path'
import * as fs from 'fs-extra'
import * as chalk from 'chalk'

export default class Docker {
  out: Output
  config: Config
  ymlPath: string = path.join(__dirname, 'docker/docker-compose.yml')
  envPath: string = path.join(__dirname, 'docker/.envrc')
  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }

  async up(): Promise<any> {
    await this.run('up', '-d', '--remove-orphans')
    return this.getDockerEnvVars()
  }

  start() {
    return this.run('start')
  }

  stop() {
    return this.run('stop')
  }

  restart() {
    return this.run('restart')
  }

  pull() {
    return this.run('pull', '--parallel')
  }

  private run(...argv: string[]): Promise<number> {
    return new Promise((resolve, reject) => {
      const dockerEnv = this.getDockerEnvVars()
      const env = { ...process.env, ...dockerEnv }
      const defaultArgs = ['--file', this.ymlPath, '--project-directory', this.config.cwd]
      const args = defaultArgs.concat(argv)
      this.out.log(chalk.dim(`$ docker-compose ${argv.join(' ')}\n`))
      const child = childProcess.spawn('docker-compose', args, {
        env,
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
          resolve(code)
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
