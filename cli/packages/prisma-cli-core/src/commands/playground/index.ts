import { Command, flags, Flags } from 'prisma-cli-engine'
import * as opn from 'opn'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'
const debug = require('debug')('playground')
import * as path from 'path'
import * as os from 'os'
import * as crypto from 'crypto'
import getGraphQLCliBin from '../../utils/getGraphQLCliBin'
import chalk from 'chalk'
import { spawn } from '../../spawn'
import * as express from 'express'
import * as requestProxy from 'express-request-proxy'
import expressPlayground from 'graphql-playground-middleware-express'
import { getGraphQLConfig, GraphQLConfig } from 'graphql-config'
import {
  makeConfigFromPath,
  patchEndpointsToConfig,
} from 'graphql-config-extension-prisma'

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
    'env-file': flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
    'server-only': flags.boolean({
      char: 's',
      description: 'Run only the server',
    }),
    port: flags.number({
      char: 'p',
      defaultValue: 3000,
      description: 'Port to serve the Playground web version on',
    }),
  }
  async run() {
    const { web, port } = this.flags
    const envFile = this.flags['env-file']
    const serverOnly = this.flags['server-only']
    await this.definition.load(this.flags, envFile)

    const serviceName = this.definition.service!
    const stage = this.definition.stage!
    const workspace = this.definition.getWorkspace()
    const cluster = this.definition.getCluster()

    const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

    const isLocalPlaygroundAvailable = fs.existsSync(localPlaygroundPath)

    const shouldStartServer = serverOnly || web || !isLocalPlaygroundAvailable

    const shouldOpenBrowser = !serverOnly

    const config = await this.getConfig()

    if (shouldStartServer) {
      const endpoint = cluster!.getApiEndpoint(
        this.definition.service!,
        stage,
        this.definition.getWorkspace() || undefined,
      )
      const link = await this.startServer({ config, endpoint, port })

      if (shouldOpenBrowser) {
        opn(link)
      }
    } else {
      const envPath = path.join(os.tmpdir(), `${randomString()}.json`)
      fs.writeFileSync(envPath, JSON.stringify(process.env))
      const url = `graphql-playground://?cwd=${process.cwd()}&envPath=${envPath}`
      opn(url, { wait: false })
    }

    // if (this.config.findConfigDir() === this.config.definitionDir) {
    //   const graphqlBin = await getGraphQLCliBin()
    //   debug({ graphqlBin })
    //   this.out.log(`Running ${chalk.cyan(`$ graphql playground`)}...`)
    //   const args = ['playground']
    //   if (web) {
    //     args.push('--web')
    //   }
    //   await spawn(graphqlBin, args)
    // } else {
    //   const endpoint = cluster!.getApiEndpoint(
    //     this.definition.service!,
    //     stage,
    //     this.definition.getWorkspace() || undefined,
    //   )
    //   if (fs.pathExistsSync(localPlaygroundPath) && !web) {
    //     const envPath = path.join(os.tmpdir(), `${randomString()}.json`)
    //     fs.writeFileSync(envPath, JSON.stringify(process.env))
    //     const url = `graphql-playground://?cwd=${
    //       this.config.cwd
    //     }&envPath=${envPath}&endpoint=${endpoint}`
    //     opn(url, { wait: false })
    //     debug(url)
    //     debug(process.env)
    //   } else {
    //     opn(endpoint)
    //   }
    // }
  }

  async getConfig(): Promise<GraphQLConfig> {
    try {
      return await patchEndpointsToConfig(
        getGraphQLConfig(this.config.cwd),
        this.config.cwd,
      )
    } catch (e) {
      return makeConfigFromPath() as any
    }
  }

  startServer = async ({
    config,
    endpoint,
    port = 3000,
  }: {
    config
    endpoint: string
    port: string
  }) =>
    new Promise<string>(async (resolve, reject) => {
      const app = express()
      const projects = config.getProjects()

      if (projects === undefined) {
        const { url, headers } = config.endpointsExtension.getEndpoint(endpoint)

        app.use(
          '/graphql',
          requestProxy({
            url,
            headers,
          }),
        )

        app.use(
          '/playground',
          expressPlayground({
            endpoint: '/graphql',
            config: config.config,
          }),
        )
      } else {
        app.use(
          '/playground',
          expressPlayground({ config: config.config } as any),
        )
      }

      const listener = app.listen(port, () => {
        let host = listener.address().address
        if (host === '::') {
          host = 'localhost'
        }
        const link = `http://${host}:${port}/playground`
        console.log('Serving playground at %s', chalk.blue(link))

        resolve(link)
      })
    })
}
