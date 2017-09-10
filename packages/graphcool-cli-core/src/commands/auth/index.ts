import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as chalk from 'chalk'

export default class Auth extends Command {
  static topic = 'auth'
  static description = 'Sign up or login (opens your browser for authentication)'
  static help = `
    
  Note: Your session token will be store at ~/.graphcool
  
  ${chalk.blue('Examples:')}
      
  ${chalk.gray('-')} Authenticate using the browser
    ${chalk.blue('$ graphcool auth')}
  
  ${chalk.gray('-')} Authenticate using an existing token
    ${chalk.blue('$ graphcool auth -t <token>')}    
  
  `
  static flags: Flags = {
    token: flags.string({
      char: 't',
      description: 'System token'
    }),
  }

  async run() {
    const {token} = this.flags

    if (token) {
      this.out.log('Using token from --token flag')
      this.auth.setToken(token)
    }

    await this.auth.ensureAuth()
  }
}
