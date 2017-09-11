import { Command, flags, Flags } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'
import * as chalk from 'chalk'

export default class FunctionsOverview extends Command {
  static topic = 'functions'
  static description = 'List all deployed functions'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id',
    }),
  }

  async run() {
    await this.auth.ensureAuth()
    let { env } = this.flags

    env = env || this.env.env.default

    const { projectId } = await this.env.getEnvironment({ env })

    if (!projectId) {
      this.out.error(
        `Please provide a valid environment that has a valid project id`,
      )
    } else {
      const functions = await this.client.getFunctions(projectId)

      if (functions.length === 0) {
        this.out.log(`There are no functions deployed for project ${chalk.bold(projectId)}`)
      } else {
        const tableHeader = [
          [
            chalk.bold('Function Name'),
            chalk.bold('Inline/Webhook'),
            chalk.bold('Type'),
            chalk.bold('Requests (last 30min)'),
            chalk.bold('Errors (last 30min)'),
          ],
        ]

        const data = functions.map(f => [
          f.name,
          f.type === 'AUTH0' ? 'inline' : 'webhook',
          prettyFunctionType(f.__typename),
          f.stats.requestCount,
          f.stats.errorCount,
        ])

        const output = table(tableHeader.concat(data), {
          border: getBorderCharacters('void'),
          columnDefault: {
            paddingLeft: '0',
            paddingRight: '2',
          },
          drawHorizontalLine: () => false,
        }).trimRight()

        this.out.log(output + '\n')
      }

    }
  }
}

function prettyFunctionType(functionType: string) {
  const types = {
    SchemaExtensionFunction: 'resolver',
    ServerSideSubscriptionFunction: 'subscription',
    RequestPipelineMutationFunction: 'request-pipeline',
  }

  return types[functionType] || functionType
}
