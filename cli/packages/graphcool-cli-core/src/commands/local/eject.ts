import { Command } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'

export default class Eject extends Command {
  static topic = 'local'
  static command = 'eject'
  static description = 'Eject from the managed docker runtime'
  static group = 'local'
  async run() {
    const newComposePath = path.join(this.config.definitionDir, 'docker-compose.yml')
    const newEnvrcPath = path.join(this.config.definitionDir, '.envrc')
    fs.copySync(path.join(__dirname, 'docker/docker-compose.yml'), newComposePath)
    fs.copySync(path.join(__dirname, 'docker/.envrc'), newEnvrcPath)
    this.out.log('')
    this.out.log(`Written ${newComposePath}`)
    this.out.log(`Written ${newEnvrcPath}\n`)
    this.out.log(`Success! To run docker on your own, you now can run

  ${chalk.green('$ direnv allow')}
    Injects the environment variables
 
  ${chalk.green('$ docker-compose up')}
    Starts the local Graphcool instance
`)
  }
}
