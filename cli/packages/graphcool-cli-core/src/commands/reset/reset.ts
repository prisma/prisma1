// import { Command, flags, Flags } from 'graphcool-cli-engine'
// import { prettyProject } from '../../util'

// export default class Reset extends Command {
//   static topic = 'reset'
//   static description = 'Reset the data of a deployed service'
//   static flags: Flags = {
//     target: flags.string({
//       char: 't',
//       description: 'Target name'
//     }),
//   }
//   async run() {
//     await this.auth.ensureAuth()
//     let {target} = this.flags

//     const {id} = await this.env.getTarget(target)

//     const info = await this.client.fetchProjectInfo(id)
//     const projectName = prettyProject(info)
//     await this.askForConfirmation(projectName)
//     this.out.action.start(`Resetting data for service ${projectName}`)
//     await this.client.resetServiceData(id)
//     this.out.action.stop()
//   }

//   private async askForConfirmation(serviceName: string) {
//     const confirmationQuestion = {
//       name: 'confirmation',
//       type: 'input',
//       message: `Are you sure that you want to reset the data of ${serviceName}? y/N`,
//       default: 'n'
//     }
//     const {confirmation}: {confirmation: string} = await this.out.prompt(confirmationQuestion)
//     if (confirmation.toLowerCase().startsWith('n')) {
//       this.out.exit(0)
//     }
//   }
// }
