import { Command, flags, Flags } from 'graphcool-cli-engine'
import {InvalidProjectError} from '../../errors/InvalidProjectError'
import * as chalk from 'chalk'

export default class FunctionLogs extends Command {
  static topic = 'logs'
  static command = 'function'
  static description = 'example command'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to set',
    }),
  }
  static args = [{
    name: 'functionName'
  }]
  async run() {
    await this.auth.ensureAuth()
    let {env} = this.flags

    env = env || this.env.env.default

    const {projectId} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else if (!this.argv[1]) {
      this.out.error(`Please provide a valid function name`)
    } else {
      const logs = await this.client.getFunctionLogs(projectId, this.argv[1])
      if (!logs) {
        this.out.log(`There is no function with the name ${this.argv[1]}. Run ${chalk.bold('graphcool functions')} to list all functions.`)
      } else if (logs.length === 0) {
        this.out.log(`No messages have been logged in the last 30 min for function ${chalk.bold(this.argv[1])}`)
      } else {
        logs.sort((a, b) => a.timestamp < b.timestamp ? -1 : 1)
        const formattedLogs = logs.map(log => {
          const prettyMessage = this.out.getStyledJSON(JSON.parse(log.message))
          return `${chalk.green.bold(log.timestamp)} ${chalk.blue.bold(`${log.duration}ms`)} ${prettyMessage}`
        }).join('\n')

        this.out.log(formattedLogs)
      }
    }
  }
}
