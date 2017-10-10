import {
  Command,
  flags,
  Flags,
  PAT,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { table, getBorderCharacters } from 'table'

export default class GetRootToken extends Command {
  static topic = 'root-token'
  static description = 'Print specified root token'
  static group = 'general'
  static flags: Flags = {
    token: flags.string({
      char: 't',
      description: 'Name of the token',
    }),
    target: flags.string({
      char: 't',
      description: 'Target name',
    }),
  }

  async run() {
    await this.auth.ensureAuth()
    const { target, token } = this.flags
    const { id } = await this.env.getTarget(target)

    this.out.action.start(`Getting root token${token ? '' : 's'}`)
    const pats = await this.client.getPats(id)
    this.out.action.stop()
    if (token) {
      const foundPat = pats.find(pat => pat.name === token)
      if (!foundPat) {
        this.out.error(
          `There is no root token with the name ${token} in project ${id}`,
        )
      } else {
        this.out.log(prettyPat(foundPat))
      }
    } else {
      if (pats.length === 0) {
        this.out.log(
          `There are no root tokens defined for project ${chalk.bold(
            id,
          )}`,
        )
      } else {
        this.out.log(
          chalk.blue(`\nRoot Tokens for project ${chalk.bold(id)}:`),
        )
        this.out.log(
          pats.map(p => chalk.gray('- ') + chalk.bold(p.name)).join('\n'),
        )
        this.out.log(
          `\n Run ${chalk.green(
            'graphcool get-root-token -t TOKEN_NAME',
          )} to receive the concrete token`,
        )
      }
    }
  }
}

const prettyPat = (pat: PAT) => `${chalk.bold(pat.name)}:\n${pat.token}`
