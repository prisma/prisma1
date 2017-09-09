import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class Trolo extends Command {
  static topic = 'trolo'
  static command = 'test'
  static description = 'this is a description'
  static help = `
  
  Example:
  $ BLA
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
    this.out.log('ran command 3')
  }
}
