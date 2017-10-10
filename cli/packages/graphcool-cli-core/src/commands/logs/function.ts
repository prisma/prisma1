import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidTargetError'
import * as chalk from 'chalk'
import * as differenceBy from 'lodash.differenceby'
import { sortByTimestamp } from '../../util'

const debug = require('debug')('logs')

export default class FunctionLogs extends Command {
  static topic = 'logs'
  static description = 'Log functions'
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
    function: flags.string({
      char: 'f',
      description: 'Name of the function to get the logs from',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    const { tail } = this.flags
    let { env } = this.flags
    const functionName = this.flags.function

    env = env || this.env.env.default

    const { projectId } = await this.env.getEnvironment({ env })
    debug(`function name ${functionName}`)

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else if (!functionName) {
      this.out.error(`Please provide a valid function name`)
    } else {
      let fn = await this.client.getFunction(projectId, functionName)
      if (!fn) {
        this.out.error(
          `There is no function with the name ${functionName}. Run ${chalk.bold(
            'graphcool functions',
          )} to list all functions.`,
        )
      } else {
        let logs = (await this.client.getFunctionLogs(fn.id)) || []
        if (logs.length === 0) {
          this.out.log(
            `No messages have been logged in the last 30 min for function ${chalk.bold(
              functionName,
            )}`,
          )
        } else {
          logs.sort(sortByTimestamp)
          this.out.log(this.prettifyLogs(logs))
        }

        if (tail) {
          setInterval(async () => {
            const tailLogs = await this.client.getFunctionLogs(fn!.id, 50)
            if (tailLogs === null) {
              fn = await this.client.getFunction(projectId, functionName)
            } else {
              if (tailLogs.length > 0) {
                const newLogs = differenceBy(tailLogs, logs, l => l.id)
                if (newLogs.length > 0) {
                  newLogs.sort(sortByTimestamp)
                  this.out.log(this.prettifyLogs(newLogs))
                  logs = logs.concat(newLogs)
                }
              }
            }
          }, 4000)
        }
      }
    }
  }
  private prettifyLogs(logs: any) {
    return logs
      .map(log => {
        const json = JSON.parse(log.message)
        if (json.event) {
          try {
            json.event = JSON.parse(json.event)
          } catch (e) {
            // noop
          }
        }

        const styleLog = (l: string) => {
          let potentialJson = l.slice(26)
          try {
            potentialJson = JSON.parse(potentialJson)
          } catch (e) {
            // noop
          }

          return {
            [l.slice(0, 26)]: potentialJson,
          }
        }

        if (json.logs) {
          json.logs = json.logs.map(styleLog)
        }

        const prettyMessage = this.out.getStyledJSON(json)
        const status = log.status === 'SUCCESS' ? 'green' : 'red'
        return `${chalk.cyan.bold(log.timestamp)} ${chalk.blue.bold(
          `${log.duration}ms`,
        )} ${chalk.bold[status](log.status)} ${prettyMessage}`
      })
      .join('\n')
  }
}
