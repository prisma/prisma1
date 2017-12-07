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
    }),
    web: flags.boolean({
      char: 'w',
      description: 'Force open web playground',
    }),
  }
  async run() {
    const { stage, web } = this.flags
    await this.definition.load(this.env, this.flags)
    const clusterName = this.definition.getStage(stage, true)
    const cluster = this.env.clusterByName(clusterName!, true)!

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`
    const stageName = stage || this.definition.rawStages.default

    const endpoint = cluster.getApiEndpoint(
      this.definition.definition!.service!,
      stageName,
    )
    if (fs.pathExistsSync(localPlaygroundPath) && !web) {
      const url = `graphql-playground://?endpoint=${
        endpoint
      }&cwd=${process.cwd()}&env=${JSON.stringify(process.env)}`
      opn(url)
    } else {
      opn(endpoint)
    }
  }
}
