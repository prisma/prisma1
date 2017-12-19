import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open service endpoints in GraphQL Playground'
  static group = 'general'
  static flags: Flags = {
    stage: flags.string({
      char: 's',
      description: 'Stage name',
      defaultValue: 'dev',
    }),
    web: flags.boolean({
      char: 'w',
      description: 'Force open web playground',
    }),
  }
  async run() {
    const { stage, web } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service!
    const cluster = await this.client.getClusterSafe(serviceName, stage)

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

    const endpoint = cluster.getApiEndpoint(
      this.definition.definition!.service!,
      stage,
    )
    if (fs.pathExistsSync(localPlaygroundPath) && !web) {
      const url = `graphql-playground://?endpoint=${endpoint}&cwd=${process.cwd()}&env=${JSON.stringify(
        process.env,
      )}`
      opn(url)
    } else {
      opn(endpoint)
    }
  }
}
