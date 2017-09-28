import {
  Command,
  EnvironmentConfig,
  Flags,
  flags,
  ProjectInfo,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { printPadded, subscriptionURL } from '../../util'

export default class Info extends Command {
  static topic = 'info'
  static description = 'Print project info (environments, endpoints, ...) '
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let { env } = this.flags

    env = env || this.env.env.default

    const { projectId, envName } = await this.env.getEnvironment({ env })

    if (!projectId) {
      this.out.error(
        `Please provide a valid environment that has a valid project id`,
      )
    } else {
      const info = await this.client.fetchProjectInfo(projectId)
      this.out.log(infoMessage(info, this.env.env, env))
    }
  }
}

export const infoMessage = (
  info: ProjectInfo,
  env: EnvironmentConfig,
  envName: string,
) => `\

${chalk.bold('Local Environments')}

${printEnvironments(env)}

${chalk.bold(`Selected Environment (${envName})`)}

  Project Name     ${chalk.bold(info.name)}

  Project ID       ${chalk.bold(info.id)}
  
  Endpoints
    
    Simple         ${chalk.underline(
      `https://api.graph.cool/simple/v1/${info.id}`,
    )}
  
    Relay          ${chalk.underline(
      `https://api.graph.cool/relay/v1/${info.id}`,
    )}
    
    Subscriptions  ${chalk.underline(
      subscriptionURL(info.region as any, info.id),
    )}
    
    File           ${chalk.underline(
      `https://api.graph.cool/file/v1/${info.id}`,
    )}
`

const printEnvironments = (env: EnvironmentConfig) => {
  return printPadded(
    Object.keys(env.environments).map(key => [
      key,
      `${chalk.dim(`\`${env.environments[key]}\``)}`,
    ]),
  )
}
