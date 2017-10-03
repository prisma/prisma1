import {
  Command,
  EnvironmentConfig,
  Flags,
  flags,
  ProjectInfo,
  Output,
  Project,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { prettyProject, printPadded, subscriptionURL } from '../../util'

export default class InfoCommand extends Command {
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
    const projects: Project[] = await this.client.fetchProjects()

    if (!projectId) {
      this.out.error(
        `Please provide a valid environment that has a valid project id`,
      )
    } else {
      const info = await this.client.fetchProjectInfo(projectId)
      this.out.log(infoMessage(info, this.env.env, env, this.config.backendAddr, this.out, projects))
    }
  }
}

export const infoMessage = (
  info: ProjectInfo,
  env: EnvironmentConfig,
  envName: string,
  backendAddr: string,
  out: Output,
  projects: Project[]
) => `\

${printEnvironments(env, projects, out)}
 
API:           Endpoint:
────────────── ────────────────────────────────────────────────────────────
${chalk.green('Simple')}         ${
      `${backendAddr}/simple/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`
    }
${chalk.green('Relay')}          ${
  `${backendAddr}/relay/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`
}
${chalk.green('Subscriptions')}  ${
  subscriptionURL(info.region as any, info.id)
}
${chalk.green('File')}           ${
  `${backendAddr}/file/${backendAddr.includes('localhost') ? '': 'v1/'}${info.id}`
}
`

const printEnvironments = (env: EnvironmentConfig, projects: Project[], out: Output) => {
  return printPadded(
    Object.keys(env.environments).map(key => {
      let projectId: any = env.environments[key]
      if (typeof projectId === 'object') {
        projectId = projectId.projectId
      }
      const project = projects.find(p => p.id === projectId)
      let output = `${chalk.bold('local')} (${projectId})`
      if (project) {
        output = prettyProject(project)
      }
      return [
        key,
        output,
      ]
    }),
    0, 1,
    ['Environment:', 'Project:']
  )
}
