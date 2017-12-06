// import {
//   Command,
//   Flags,
//   flags,
//   ProjectInfo,
//   Output,
//   Project,
//   Targets,
// } from 'graphcool-cli-engine'
// import chalk from 'chalk'

// export default class InfoCommand extends Command {
//   static topic = 'info'
//   static description = 'Display service information (endpoints, cluster, ...)'
//   static group = 'general'
//   static flags: Flags = {
//     stage: flags.string({
//       char: 't',
//       description: 'Target name to get the info for',
//     }),
//   }
//   async run() {
//     let { stage } = this.flags

//     const { id } = await this.env.getTarget(stage)
//     const stageName = stage || 'default'

//     await this.auth.ensureAuth()

//     const projects: Project[] = await this.client.fetchProjects()

//     const info = await this.client.fetchProjectInfo(id)
//     let localPort: number | undefined = parseInt(
//       this.env.clusterEndpoint.split(':')[1],
//       10,
//     )
//     if (this.env.rc.stages) {
//       this.out.log(
//         this.infoMessage(
//           info,
//           stageName,
//           projects,
//           localPort,
//         ),
//       )
//     } else {
//       this.out.log(`No local stages`)
//     }
//   }
//   infoMessage = (
//     info: ProjectInfo,
//     envName: string,
//     projects: Project[],
//     localPort?: number,
//   ) => `\

// ${this.out.printServices(this.env.rc.stages!, projects)}

// API:           Endpoint:
// ────────────── ────────────────────────────────────────────────────────────
// ${chalk.green('Simple')}         ${this.env.simpleEndpoint(info.id)}
// ${chalk.green('Relay')}          ${this.env.relayEndpoint(info.id)}
// ${chalk.green('Subscriptions')}  ${this.env.subscriptionEndpoint(info.id)}
// ${chalk.green('File')}           ${this.env.fileEndpoint(info.id)}
// `
// }
