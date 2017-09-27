import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as chalk from 'chalk'

export default class Auth extends Command {
  static topic = 'auth'
  static description = 'Sign up or login (opens your browser for authentication)'
  static help = `
    
  Note: Your session token will be store at ~/.graphcool
  
  ${chalk.green('Examples:')}
      
  ${chalk.gray('-')} Authenticate using the browser
    ${chalk.green('$ graphcool auth')}
  
  ${chalk.gray('-')} Authenticate using an existing token
    ${chalk.green('$ graphcool auth -t <token>')}    
  
  `
  static flags: Flags = {
    token: flags.string({
      char: 't',
      description: 'System token',
    }),
  }

  async run() {
    const { token } = this.flags

    if (token) {
      this.out.log('Using token from --token flag')
      this.auth.setToken(token)
    }

    const alreadyAuthenticated = await this.auth.ensureAuth()

    if (alreadyAuthenticated) {
      this.out.log(
        `You are already authenticated. Your local token is saved at ${chalk.bold(
          this.config.dotGraphcoolFilePath,
        )}`,
      )
    }
  }
}
