import { Command, flags, Flags, GraphcoolModule } from 'graphcool-cli-engine'
import { flatMap, isEqual } from 'lodash'
import { LocalInvoker } from '../../LocalInvoker'
import * as path from 'path'
import * as fs from 'fs-extra'
import { sortByTimestamp } from '../../util'
import chalk from 'chalk'

export default class InvokeLocal extends Command {
  static topic = 'invoke-local'
  // static command = 'local'
  static description = 'Invoke a function locally'
  static group = 'general'
  static flags: Flags = {
    function: flags.string({
      char: 'f',
      description: 'Name of the function',
      required: true,
    }),
    json: flags.string({
      char: 'j',
      description: 'Path to the function input .json file',
    }),
    lastEvent: flags.boolean({
      char: 'l',
      description:
        'Download the input event of the last execution to events/FUNCTION/event.json and invoke the function',
    }),
    target: flags.string({
      char: 't',
      description: 'Target to be deployed',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let { target } = this.flags
    const { project } = this.flags
    const fnName = this.flags.function

    const { id } = await this.env.getTarget(target)
    await this.definition.load(this.flags)

    const result = this.definition.getFunctionAndModule(fnName)
    if (!result) {
      this.out.error(
        `Function "${fnName}" couldn't be found in graphcool.yml or local modules`,
      )
    } else if (
      !result.fn.handler ||
      !result.fn.handler.code
    ) {
      this.out.error(
        `Function "${name}" doesn't have a code handler defined, so it can't be executed locally`,
      )
    } else {
      const { fn, module } = result
      // gather event
      const json = await this.getEvent(id, fnName)

      // TODO rm any
      const invoker = new LocalInvoker(
        this.config,
        this.env,
        this.out,
        module,
        fnName,
        fn as any,
      )

      const invocationResult = await invoker.invoke(json)
      this.out.log(chalk.bold.magenta(`\nResult:\n`))
      this.out.log(this.out.getStyledJSON(invocationResult))
    }
  }

  private async getEvent(projectId: string, fnName: string) {
    const { json, lastEvent } = this.flags

    if (!json && !lastEvent) {
      this.out.error(
        `Please provider either --json or --lastEvent to invoke the function locally`,
      )
    }

    let event = {}
    if (json) {
      const jsonPath = path.join(this.config.definitionDir, json)
      if (!fs.pathExistsSync(jsonPath)) {
        this.out.error(`Provided json path ${jsonPath} doesn't exist`)
      } else {
        const jsonFile = fs.readFileSync(jsonPath, 'utf-8')
        try {
          event = JSON.parse(jsonFile)
        } catch (e) {
          this.out.error(`Can't parse json of file ${jsonFile}: ` + e.message)
        }
      }
    }

    if (lastEvent) {
      const lastEventJson = await this.getLastEvent(projectId, fnName)
      const examplesDir = path.join(
        this.config.definitionDir,
        `events/${fnName}/`,
      )
      fs.mkdirpSync(examplesDir)

      let count = 1
      while (fs.pathExistsSync(path.join(examplesDir, `${count}.json`))) {
        count++
      }
      const lastEventPath = path.join(examplesDir, `${count - 1}.json`)
      const lastSavedEvent = fs.pathExistsSync(lastEventPath) ? fs.readJsonSync(lastEventPath) : null

      let relativeLastEventPath
      if (count > 1 && isEqual(lastEventJson, lastSavedEvent)) {
        relativeLastEventPath = `events/${fnName}/${count - 1}.json`
        this.out.log(
          chalk.blue(
            `Using last event of ${chalk.bold(
              fnName,
            )} which is already written to ${chalk.bold(
              relativeLastEventPath,
            )}\n`,
          ),
        )
        event = lastEventJson
      } else {
        const examplePath = path.join(examplesDir, `${count}.json`)
        fs.writeFileSync(examplePath, JSON.stringify(lastEventJson, null, 2))
        relativeLastEventPath = `events/${fnName}/${count}.json`
        this.out.log(
          chalk.blue(
            `Written last event of ${chalk.bold(fnName)} to ${chalk.bold(
              relativeLastEventPath,
            )}`,
          ),
        )
        this.out.log(
          chalk.blue(
            `To customize the input and have a faster execution, you from now on can use`,
          ),
        )
        this.out.log(
          chalk.blue.bold(
            `graphcool invoke local -f ${fnName} -j ${relativeLastEventPath}\n`,
          ),
        )
        event = lastEventJson
      }
    }

    if (!event) {
      this.out.error(`Could not get an input event for the function execution`)
    }

    return event
  }

  private async getLastEvent(projectId: string, fnName: string) {
    const fn = await this.client.getFunction(projectId, fnName)
    if (!fn) {
      this.out.error(
        `Function "${fnName}" is not deployed in project ${projectId}`,
      )
    } else {
      const logs = await this.client.getFunctionLogs(fn.id, 5)
      if (!logs) {
        this.out.error(`There are no logged events for function ${fnName}`)
      } else {
        logs.sort(sortByTimestamp).reverse()

        const foundLog = logs
          .map(log => {
            const json = JSON.parse(log.message)
            if (json.event) {
              try {
                json.event = JSON.parse(json.event)
              } catch (e) {
                // noop
              }
            }

            return json
          })
          .find(log => {
            return log && log.event
          })

        if (foundLog) {
          return foundLog.event
        } else {
          this.out.error(
            `Could not find a valid event for function ${fnName} in the logs of project ${projectId}`,
          )
        }
      }
    }
  }
}
