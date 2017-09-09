import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as chalk from 'chalk'

export default class Deploy extends Command {
  static topic = 'deploy'
  static description = 'Deploy project definition changes'
  static help = `
  
  ${chalk.blue('Examples:')}
      
${chalk.gray('-')} Deploy local changes from graphcool.yml to the default project environment.
  ${chalk.blue('$ graphcool deploy')}

${chalk.gray('-')} Deploy local changes to a specific environment
  ${chalk.blue('$ graphcool deploy --env production')}
    
${chalk.gray('-')} Deploy local changes from default project file accepting potential data loss caused by schema changes
  ${chalk.blue('$ graphcool deploy --force --env production')}
  `
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Project environment to be deployed'
    }),
    project: flags.string({
      char: 'p',
      description: 'ID or alias of  project to deploy'
    }),
    force: flags.string({
      char: 'f',
      description: 'Accept data loss caused by schema changes'
    }),
  }
  async run() {
    this.out.log('Trolo', this.flags, this.args, this.argv)
  }
}
