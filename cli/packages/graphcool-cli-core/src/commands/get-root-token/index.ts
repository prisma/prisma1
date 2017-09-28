import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  PAT,
} from 'graphcool-cli-engine'
import { ProjectDoesntExistError } from '../../errors/ProjectDoesntExistError'
import * as chalk from 'chalk'
import { table, getBorderCharacters } from 'table'

export default class GetRootToken extends Command {
  static topic = 'get-root-token'
  static description = 'Get the root tokens of a specific prject'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to set',
    }),
    token: flags.string({
      char: 't',
      description: 'Name of the token',
    }),
  }

  async run() {
    await this.auth.ensureAuth()
    const {env, project, token} = this.flags
    const {projectId} = await this.env.getEnvironment({
      project,
      env,
    })

    if (!projectId) {
      if (project) {
        this.out.error(new ProjectDoesntExistError(project))
      }

      if (env) {
        this.out.error(new EnvDoesntExistError(env))
      }

      this.out.error(
        `Please provide either a default environment, a project or an environment you want to get the root token from.`,
      )
    } else {
      this.out.action.start(`Getting root token${token ? '' : 's'}`)
      const pats = await this.client.getPats(projectId)
      this.out.action.stop()
      if (token) {
        const foundPat = pats.find(pat => pat.name === token)
        if (!foundPat) {
          this.out.error(`There is no root token with the name ${token} in project ${projectId}`)
        } else {
          this.out.log(prettyPat(foundPat))
        }
      } else {

        if (pats.length === 0) {
          this.out.log(`There are no root tokens defined for project ${chalk.bold(projectId)}`)
        } else {

          this.out.log(chalk.blue(`\nRoot Tokens for project ${chalk.bold(projectId)}:`))
          this.out.log(pats.map(p => chalk.gray('- ') + chalk.bold(p.name)).join('\n'))
          this.out.log(`\n Run ${chalk.green('graphcool get-root-token -t TOKEN_NAME')} to receive the concrete token`)

        }
      }
    }
  }
}

const prettyPat = (pat: PAT) => `${chalk.bold(pat.name)}:\n${pat.token}`
