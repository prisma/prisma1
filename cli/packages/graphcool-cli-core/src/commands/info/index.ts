import {
  Command,
  Flags,
  flags,
  ProjectInfo,
  Output,
  Project,
  Targets,
} from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class InfoCommand extends Command {
  static topic = 'info'
  static description = 'Display service information (endpoints, cluster, ...)'
  static group = 'general'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name to get the info for',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let { target } = this.flags

    const { id } = await this.env.getTarget(target)
    const targetName = target || 'default'

    const projects: Project[] = await this.client.fetchProjects()

    const info = await this.client.fetchProjectInfo(id)
    let localPort: number | undefined = parseInt(
      this.env.clusterEndpoint.split(':')[1],
      10,
    )
    if (this.env.rc.targets) {
      this.out.log(
        this.infoMessage(
          info,
          targetName,
          projects,
          localPort,
        ),
      )
    } else {
      this.out.log(`No local targets`)
    }
  }
  infoMessage = (
    info: ProjectInfo,
    envName: string,
    projects: Project[],
    localPort?: number,
  ) => `\

${this.out.printServices(this.env.rc.targets!, projects)}
 
API:           Endpoint:
────────────── ────────────────────────────────────────────────────────────
${chalk.green('Simple')}         ${this.env.simpleEndpoint(info.id)}
${chalk.green('Relay')}          ${this.env.relayEndpoint(info.id)}
${chalk.green('Subscriptions')}  ${this.env.subscriptionEndpoint(info.id)}
${chalk.green('File')}           ${this.env.fileEndpoint(info.id)}
`
}
