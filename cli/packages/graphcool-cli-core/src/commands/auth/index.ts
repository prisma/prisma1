import { Command, flags, Flags } from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class Auth extends Command {
  static topic = 'login'
  static description = 'Login or signup to the Graphcool Platform'
  static group = 'platform'
  static help = `
    
  Note: Your session token will be store at ~/.graphcool
  
  ${chalk.green('Examples:')}
      
  ${chalk.gray('-')} Authenticate using the browser
    ${chalk.green('$ graphcool login')}
  
  ${chalk.gray('-')} Authenticate using an existing token
    ${chalk.green('$ graphcool login -t <token>')}    
  
  `
  static flags: Flags = {
    token: flags.string({
      char: 'T',
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

    // if there is a new token, save it
    if (token) {
      this.env.setToken(token)
      this.env.saveGlobalRC()
      this.out.log(
        `Saved new token to ${chalk.bold(this.config.globalRCPath)}`,
      )
    } else if (alreadyAuthenticated) {
      this.out.log(
        `You are already authenticated. Your local token is saved at ${chalk.bold(
          this.config.globalRCPath,
        )}`,
      )
    }
  }
}
