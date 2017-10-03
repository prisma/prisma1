import {
  Command,
  EnvironmentConfig,
  Flags,
  flags,
  ProjectInfo,
  Output,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { printPadded, subscriptionURL } from '../../util'

export default class ProjectInfoCommand extends Command {
  static topic = 'projects'
  static command = 'info'
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
      this.out.log(infoMessage(info, this.env.env, env, this.config.backendAddr, this.out))
    }
  }
}

export const infoMessage = (
  info: ProjectInfo,
  env: EnvironmentConfig,
  envName: string,
  backendAddr: string,
  out: Output,
) => `\

${chalk.bold('Environments')}

${printEnvironments(env, out)}

${chalk.bold(`Selected Environment (${envName})`)}

  Project Name     ${chalk.bold(info.name)}

  Project ID       ${chalk.bold(info.id)}
  
  Endpoints
    
    Simple         ${chalk.underline(
      `${backendAddr}/simple/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`,
    )}
  
    Relay          ${chalk.underline(
      `${backendAddr}/relay/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`,
    )}
    
    Subscriptions  ${chalk.underline(
      subscriptionURL(info.region as any, info.id),
    )}
    
    File           ${chalk.underline(
      `${backendAddr}/file/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`,
    )}
`

const printEnvironments = (env: EnvironmentConfig, out: Output) => {
  return printPadded(
    Object.keys(env.environments).map(key => {
      let output: any = env.environments[key]
      if (typeof output === 'object') {
        output = out.getStyledJSON({...env.environments[key] as any, token: 'XXX'})
        const lines = output.split('\n')
        output = lines.slice(0, 1).concat(lines.slice(1).map(l => `  ${l}`)).join('\n').trim()
      } else {
        output = `\`${output}\``
      }
      return [
        key,
        `${chalk.dim(output)}`,
      ]
    }),
  )
}
