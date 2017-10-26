import { Command, flags, Flags } from 'graphcool-cli-engine'
import chalk from 'chalk'
import * as differenceBy from 'lodash.differenceby'
import { sortByTimestamp } from '../../util'
import {flatMap} from 'lodash'

const debug = require('debug')('logs')

export default class FunctionLogs extends Command {
  static topic = 'logs'
  static description = 'Output service logs'
  static group = 'general'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target to get logs from',
    }),
    tail: flags.boolean({
      description: 'Tail function logs in realtime',
    }),
    function: flags.string({
      char: 'f',
      description: 'Name of the function to get the logs from',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    const { tail, target } = this.flags
    const functionName = this.flags.function

    const {id} = await this.env.getTarget(target)
    debug(`function name ${functionName}`)
    debug(`project id ${id}`)

    if (!functionName) {
      await this.provideAllFunctionLogs(id, tail)
    } else {
      await this.provideSingleFunctionLogs(id, functionName, tail)
    }
  }
  private async provideAllFunctionLogs(id: string, tail?: boolean) {
      let logs = (await this.client.getAllFunctionLogs(id)) || []
      if (logs.length === 0) {
        this.out.log(
          `No messages have been logged in the last 30 min for project ${chalk.bold(
            id,
          )}`,
        )
      } else {
        logs.sort(sortByTimestamp)
        this.out.log(this.prettifyLogs(logs))
      }

      if (tail) {
        setInterval(async () => {
          const tailLogs = await this.client.getAllFunctionLogs(id, 50)
          if (tailLogs) {
            if (tailLogs.length > 0) {
              const newLogs = differenceBy(tailLogs, logs, l => l.id)
              if (newLogs.length > 0) {
                newLogs.sort(sortByTimestamp)
                this.out.log(this.prettifyLogs(newLogs))
                logs = logs.concat(newLogs)
              }
            }
          } else {
            this.out.log(`Project ${id} can't be found anymore`)
          }
        }, 4000)
      }
  }
  private async provideSingleFunctionLogs(id: string, functionName: string, tail?: boolean) {
    let fn = await this.client.getFunction(id, functionName)
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
            fn = await this.client.getFunction(id, functionName)
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
        //
        // const styleLog = (l: string) => {
        //   const logs = this.lambdaToArray(l)
        //   let potentialJson = l.slice(62).trim()
        //   try {
        //     potentialJson = JSON.parse(potentialJson)
        //   } catch (e) {
        //     // noop
        //   }
        //
        //   return {
        //     [l.slice(0, 24)]: potentialJson,
        //   }
        // }

        // if (json.logs) {
        //   json.logs = flatMap(json.logs.map(this.lambdaToArray)).map(styleLog)
        // }

        const prettyMessage = this.out.getStyledJSON(json)
        const status = log.status === 'SUCCESS' ? 'green' : 'red'
        return `${chalk.cyan.bold(log.timestamp)} ${chalk.blue.bold(
          `${log.duration}ms`,
        )} ${chalk.bold[status](log.status)} ${prettyMessage}`
      })
      .join('\n')
  }

  private lambdaToArray(logs: string): string[] {
    logs = logs.replace(/\t/g, '  ')

    const regex = /\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+.*/

    const lines = logs.split('\n')
      .filter(l => !l.startsWith('START') && !l.startsWith('END') && !l.startsWith('REPORT'))

    const merged = lines
      .reduce((acc, curr, index) => {
        if (lines[index + 1] && lines[index + 1].match(regex)) {
          return {
            lines: acc.lines.concat(acc.currentLine + (acc.currentLine.length > 0 ? '\n' : '') + curr),
            currentLine: ''
          }
        } else {
          return {
            lines: acc.lines,
            currentLine: acc.currentLine + (acc.currentLine.length > 0 ? '\n' : '') + curr,
          }
        }
      }, {
        lines: [] as any,
        currentLine: ''
      })

    return merged.lines.concat(merged.currentLine)
  }
}
