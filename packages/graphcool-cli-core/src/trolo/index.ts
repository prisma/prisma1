import {Command} from 'graphcool-cli-engine'

export default class Trolo extends Command {
  static topic = 'trolo'
  static command = 'test'
  static description = 'this is a description'
  static help = `
  
  Example:
  $ BLA
  `
  async run() {
    this.out.log('ran command 2')
  }
}
