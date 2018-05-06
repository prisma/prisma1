import { Command } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import Docker from './Docker'

export default class Eject extends Command {
  static topic = 'local'
  static command = 'eject'
  static description = 'Eject from the managed docker runtime'
  // static group = 'local'
  static hidden = true
  async run() {
    const newComposePath = path.join(this.config.cwd, 'docker-compose.yml')
    fs.copySync(
      path.join(__dirname, 'docker/docker-compose.yml'),
      newComposePath,
    )
    let newEnvrcPath

    const isWin = /^win/.test(process.platform)
    // const isWin = true
    if (isWin) {
      newEnvrcPath = path.join(this.config.cwd, 'env.bat')
      fs.writeFileSync(newEnvrcPath, this.getWindowsBat())
    } else {
      newEnvrcPath = path.join(this.config.cwd, 'env')
      fs.copySync(path.join(__dirname, 'docker/env'), newEnvrcPath)
    }
    this.out.log('')
    this.out.log(`Written ${newComposePath}`)
    this.out.log(`Written ${newEnvrcPath}\n`)
    this.out.log(`Success! To run docker on your own, you now can run

  ${chalk.green('$ direnv allow')}
    Injects the environment variables
 
  ${chalk.green('$ docker-compose up')}
    Starts the local Prisma instance
`)
  }

  private getWindowsBat() {
    const envrc = fs.readFileSync(path.join(__dirname, 'docker/env'), 'utf-8')
    const docker = new Docker(this.out, this.config, this.env, '')
    const env = docker.parseEnv(envrc)
    return this.printBat(env)
  }

  private printBat(env: { [key: string]: string }): string {
    return Object.keys(env)
      .map(key => {
        const value = env[key]

        return `set ${key}=${value}`
      })
      .join('\n')
  }
}
