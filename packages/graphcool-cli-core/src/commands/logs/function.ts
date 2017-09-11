import { Command, flags, Flags } from 'graphcool-cli-engine'
import {InvalidProjectError} from '../../errors/InvalidProjectError'
import * as chalk from 'chalk'
import * as sortedUniqBy from 'lodash.sorteduniqby'

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
    tail: flags.boolean({
      char: 't',
      description: 'Tail function logs in realtime',
    }),
  }
  static args = [{
    name: 'functionName'
  }]
  async run() {
    await this.auth.ensureAuth()
    const {tail} = this.flags
    let {env} = this.flags

    env = env || this.env.env.default

    const {projectId} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else if (!this.argv[1]) {
      this.out.error(`Please provide a valid function name`)
    } else {
      const fn = await this.client.getFunction(projectId, this.argv[1])
      if (!fn) {
        this.out.error(`There is no function with the name ${this.argv[1]}. Run ${chalk.bold('graphcool functions')} to list all functions.`)
      } else {
        const {logs, endCursor} = await this.client.getFunctionLogs(fn.id)
        let lastCursor = endCursor
        if (logs.length === 0) {
          this.out.log(`No messages have been logged in the last 30 min for function ${chalk.bold(this.argv[1])}`)
        } else {
          logs.sort(sortByTimestamp)
          this.out.log(this.prettifyLogs(logs))
        }


        if (tail) {
          setInterval(async () => {
            const tailResult = await this.client.getFunctionLogs(fn.id, lastCursor)
            if (tailResult.logs && tailResult.logs.length > 0) {
              tailResult.logs.sort(sortByTimestamp)
              this.out.log(this.prettifyLogs(tailResult.logs))
            }
            if (tailResult.endCursor) {
              lastCursor = tailResult.endCursor
            }
          }, 3000)
        }
      }
    }
  }
  private prettifyLogs(logs: any) {
    return logs.map(log => {
      const prettyMessage = this.out.getStyledJSON(JSON.parse(log.message))
      const status = log.status === 'SUCCESS' ? 'green' : 'red'
      return `${chalk.cyan.bold(log.timestamp)} ${chalk.blue.bold(`${log.duration}ms`)} ${chalk.bold[status](log.status)} ${prettyMessage}`
    }).join('\n')
  }
}


function sortByTimestamp(a, b) {
  return a.timestamp < b.timestamp ? -1 : 1
}
