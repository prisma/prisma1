// import {
//   Command,
//   flags,
//   Flags,
//   PAT,
// } from 'graphcool-cli-engine'
// import chalk from 'chalk'
// import { table, getBorderCharacters } from 'table'

// export default class GetRootToken extends Command {
//   static topic = 'root-token'
//   static description = 'Print specified root token'
//   static group = 'general'
//   static flags: Flags = {
//     stage: flags.string({
//       char: 't',
//       description: 'Target name',
//     }),
//   }

//   static args = [
//     {
//       name: 'token',
//       description: 'Name of the token',
//     },
//   ]

//   async run() {
//     const { stage } = this.flags
//     const { id } = await this.env.getTarget(stage)
//     const token = this.args!.token

//     await this.auth.ensureAuth()

//     const pats = await this.client.getPats(id)
//     if (token) {
//       const foundPat = pats.find(pat => pat.name === token)
//       if (!foundPat) {
//         this.out.error(
//           `There is no root token with the name ${token} in service ${id}`,
//         )
//       } else {
//         this.out.log(prettyPat(foundPat))
//       }
//     } else {
//       if (pats.length === 0) {
//         this.out.log(
//           `There are no root tokens defined for service ${chalk.bold(
//             id,
//           )}`,
//         )
//       } else {
//         this.out.log(
//           chalk.blue(`\nRoot Tokens for service ${chalk.bold(id)}:`),
//         )
//         this.out.log(
//           pats.map(p => chalk.gray('- ') + chalk.bold(p.name)).join('\n'),
//         )
//         this.out.log(
//           `\n Run ${chalk.green(
//             'graphcool root-token TOKEN_NAME',
//           )} to receive the concrete token`,
//         )
//       }
//     }
//   }
// }

// const prettyPat = (pat: PAT) => `${pat.token}`
