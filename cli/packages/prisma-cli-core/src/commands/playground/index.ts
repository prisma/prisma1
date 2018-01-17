import { Command, flags, Flags } from 'prisma-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'
const debug = require('debug')('playground')
import * as path from 'path'
import * as os from 'os'
import * as crypto from 'crypto'

function randomString(len = 32) {
  return crypto
    .randomBytes(Math.ceil(len * 3 / 4))
    .toString('base64')
    .slice(0, len)
    .replace(/\+/g, '0')
    .replace(/\//g, '0')
}

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open service endpoints in GraphQL Playground'
  static group = 'general'
  static flags: Flags = {
    web: flags.boolean({
      char: 'w',
      description: 'Force open web playground',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  async run() {
    const { web } = this.flags
    const envFile = this.flags['env-file']
    await this.definition.load(this.flags, envFile)
    const serviceName = this.definition.definition!.service!
    const stage = this.definition.definition!.stage!

    const clusterName = this.definition.getClusterName()
    const cluster = this.env.clusterByName(clusterName!, true)
    this.env.setActiveCluster(cluster!)

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

    const endpoint = cluster!.getApiEndpoint(
      this.definition.definition!.service!,
      stage,
    )
    if (fs.pathExistsSync(localPlaygroundPath) && !web) {
      const envPath = path.join(os.tmpdir(), `${randomString()}.json`)
      fs.writeFileSync(envPath, JSON.stringify(process.env))
      const url = `graphql-playground://?cwd=${process.cwd()}&envPath=${envPath}&endpoint=${endpoint}`
      opn(url, { wait: false })
      debug(url)
      debug(process.env)
    } else {
      opn(endpoint)
    }
  }
}
