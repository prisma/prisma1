import { Command, flags, Flags } from 'prisma-cli-engine'
import { EndpointDialog, DatabaseCredentials } from '../../utils/EndpointDialog'
import {
  PostgresConnector,
  PrismaDBClient,
  MongoConnector,
  Connectors,
} from 'prisma-db-introspection'
import * as path from 'path'
import * as fs from 'fs'
import { prettyTime } from '../../util'
import chalk from 'chalk'
import { Client as PGClient } from 'pg'
import { MongoClient } from 'mongodb'
import { createConnection } from 'mysql'
import { Parser, DatabaseType, Renderers, ISDL } from 'prisma-datamodel'
import { IConnector } from 'prisma-db-introspection/dist/common/connector'
import { omit } from 'lodash'
import {
  ConnectorAndDisconnect,
  getConnectedConnectorFromCredentials,
  ConnectorData,
  getConnectorWithDatabase,
  IntermediateConnectorData,
} from './util'

export default class IntrospectCommand extends Command {
  static topic = 'introspect'
  static description = 'Introspect database schema(s) of service'
  static flags: Flags = {
    interactive: flags.boolean({
      char: 'i',
      description: 'Interactive mode',
    }),
    /**
     * Postgres Params
     */
    ['pg-host']: flags.string({
      description: 'Name of the Postgres host',
    }),
    ['pg-port']: flags.string({
      description: 'The Postgres port. Default: 5432',
      defaultValue: '5432',
    }),
    ['pg-user']: flags.string({
      description: 'The Postgres user',
    }),
    ['pg-password']: flags.string({
      description: 'The Postgres password',
    }),
    ['pg-db']: flags.string({
      description: 'The Postgres database',
    }),
    ['pg-ssl']: flags.boolean({
      description: 'Enable ssl for postgres',
    }),
    ['pg-schema']: flags.string({
      description: 'Name of the Postgres schema',
    }),

    /**
     * MySQL Params
     */
    ['mysql-host']: flags.string({
      description: 'Name of the MySQL host',
    }),
    ['mysql-port']: flags.string({
      description: 'The MySQL port. Default: 3306',
      defaultValue: '3306',
    }),
    ['mysql-user']: flags.string({
      description: 'The MySQL user',
    }),
    ['mysql-password']: flags.string({
      description: 'The MySQL password',
    }),
    ['mysql-db']: flags.string({
      description: 'The MySQL database',
    }),

    /**
     * Mongo Params
     */
    ['mongo-uri']: flags.string({
      description: 'Mongo connection string',
    }),
    ['mongo-db']: flags.string({
      description: 'Mongo database',
    }),

    /**
     * Temporary flag needed to test Datamodel v2
     */
    ['prototype']: flags.boolean({
      description:
        'Output Datamodel v2. Note: This is a temporary flag for debugging',
    }),
  }
  static hidden = false
  async run() {
    /**
     * Get connector and connect to database
     */
    const connectorData = await this.getConnectorWithDatabase()

    /**
     * Introspect the database
     */

    const before = Date.now()
    this.out.action.start(
      `Introspecting database ${chalk.bold(connectorData.databaseName)}`,
    )
    const {
      sdl: newDatamodelSdl,
      numTables,
      referenceDatamodelExists,
    } = await this.introspect(connectorData)
    this.out.action.stop(prettyTime(Date.now() - before))

    /**
     * Write the result to the filesystem
     */
    const fileName = this.writeDatamodel(newDatamodelSdl)

    this.out.log(
      `Created datamodel definition based on ${numTables} database tables.`,
    )
    const andDatamodelText = referenceDatamodelExists
      ? ' and the existing datamodel'
      : ''
    this.out.log(`\
${chalk.bold(
      'Created 1 new file:',
    )}    GraphQL SDL-based datamodel (derived from existing database${andDatamodelText})

  ${chalk.cyan(fileName)}
`)

    if (this.definition.definition && !this.definition.definition!.datamodel) {
      await this.definition.load(this.flags)
      this.definition.addDatamodel(fileName)
      this.out.log(
        `Added ${chalk.bold(`datamodel: ${fileName}`)} to prisma.yml`,
      )
    }
  }

  getExistingDatamodel(databaseType: DatabaseType): ISDL | null {
    if (this.definition.typesString) {
      const ParserInstance = Parser.create(databaseType!)
      return ParserInstance.parseFromSchemaString(this.definition.typesString!)
    }

    return null
  }

  async introspect({
    connector,
    disconnect,
    databaseType,
    databaseName,
  }: ConnectorData): Promise<{
    sdl: string
    numTables: number
    referenceDatamodelExists: boolean
  }> {
    const existingDatamodel = this.getExistingDatamodel(databaseType)

    const introspection = await connector.introspect(databaseName)
    const sdl = existingDatamodel
      ? await introspection.getNormalizedDatamodel(existingDatamodel)
      : await introspection.getDatamodel()

    const renderer = Renderers.create(
      introspection.databaseType,
      this.flags.prototype,
    )
    const renderedSdl = renderer.render(sdl)

    // disconnect from database
    await disconnect()

    const numTables = sdl.types.length
    if (numTables === 0) {
      this.out.log(
        chalk.red(
          `\n${chalk.bold(
            'Error: ',
          )}The provided database doesn't contain any tables. Please provide another database.`,
        ),
      )
      this.out.exit(1)
    }

    return {
      sdl: renderedSdl,
      numTables,
      referenceDatamodelExists: Boolean(existingDatamodel),
    }
  }

  writeDatamodel(renderedSdl: string): string {
    const prototypeFileName = this.flags.prototype ? '-prototype' : ''
    const fileName = `datamodel${prototypeFileName}-${new Date().getTime()}.prisma`
    const fullFileName = path.join(this.config.definitionDir, fileName)
    fs.writeFileSync(fullFileName, renderedSdl)
    return fileName
  }

  async hasExecuteRaw() {
    try {
      await this.definition.load(this.flags)
      const service = this.definition.service!
      const stage = this.definition.stage!
      const token = this.definition.getToken(service, stage)
      const workspace = this.definition.getWorkspace()
      const introspection = await this.client.introspect(
        service,
        stage,
        token,
        workspace!,
      )
      const introspectionString = JSON.stringify(introspection)
      return introspectionString.includes('executeRaw')
    } catch (e) {
      return false
    }
  }

  /**
   * This method makes sure, that a database is present
   */
  async getConnectorWithDatabase(): Promise<ConnectorData> {
    const data = await this.getConnector()
    const endpointDialog = new EndpointDialog({
      out: this.out,
      client: this.client,
      env: this.env,
      config: this.config,
      definition: this.definition,
      shouldAskForGenerator: false,
    })

    return getConnectorWithDatabase(data, endpointDialog)
  }

  async getConnector(): Promise<IntermediateConnectorData> {
    const hasExecuteRaw = await this.hasExecuteRaw()
    const credentials = await this.getCredentials(hasExecuteRaw)
    if (credentials) {
      const {
        connector,
        disconnect,
      } = await getConnectedConnectorFromCredentials(credentials)
      return {
        connector,
        disconnect,
        databaseType: credentials.type,
        databaseName: credentials.schema,
      }
    }

    if (!hasExecuteRaw) {
      throw new Error(
        `This must not happen. No source for the introspection could be determined. Please report this issue to Prisma.`,
      )
    }

    /**
     * Continue with Prisma as the database driver, based on executeRaw
     */

    const client = new PrismaDBClient(this.definition)
    await client.connect()
    const connector = Connectors.create(client.databaseType, client)
    const disconnect = () => client.end()
    const databaseDivider =
      client.databaseType! === DatabaseType.postgres ? '$' : '@'
    const databaseName = `${this.definition.service}${databaseDivider}${
      this.definition.stage
    }`
    return {
      connector,
      disconnect,
      databaseType: client.databaseType,
      databaseName,
    }
  }

  async getCredentials(
    hasExecuteRaw: boolean,
  ): Promise<DatabaseCredentials | null> {
    const requiredPostgresFlags = ['pg-host', 'pg-user', 'pg-password', 'pg-db']
    const requiredMysqlFlags = [
      'mysql-host',
      'mysql-user',
      'mysql-password',
      'mysql-port',
    ]

    const flags = this.flags
    const flagsKeys = Object.keys(this.flags)

    const mysqlFlags = flagsKeys.filter(f => requiredMysqlFlags.includes(f))
    const postgresFlags = flagsKeys.filter(f => requiredMysqlFlags.includes(f))

    if (mysqlFlags.length > 0 && postgresFlags.length > 0) {
      throw new Error(
        `You can't provide both MySQL and Postgres connection flags. Please provide either of both.`,
      )
    }

    if (mysqlFlags.length < requiredMysqlFlags.length) {
      this.handleMissingArgs(requiredMysqlFlags, mysqlFlags, 'mysql')
    }

    if (postgresFlags.length < requiredPostgresFlags.length) {
      this.handleMissingArgs(requiredPostgresFlags, postgresFlags, 'pg')
    }

    if (mysqlFlags.length === requiredMysqlFlags.length) {
      return {
        host: flags['myqsl-host'],
        port: parseInt(flags['mysql-port'], 10),
        user: flags['mysql-user'],
        password: flags['mysql-password'],
        schema: flags['mysql-db'],
        type: DatabaseType.mysql,
      }
    }

    if (postgresFlags.length === requiredPostgresFlags.length) {
      return {
        host: flags['pg-host'],
        user: flags['pg-user'],
        password: flags['pg-password'],
        database: flags['pg-database'],
        port: parseInt(flags['pg-port'], 10),
        schema: flags['pg-schema'], // this is optional and can be undefined
        type: DatabaseType.postgres,
      }
    }

    if (flags['mongo-uri']) {
      return {
        uri: flags['mongo-uri'],
        schema: flags['mongo-db'], // this is optional and can be undefined
        type: DatabaseType.mongo,
      }
    }

    if (flags.interactive || !hasExecuteRaw) {
      const endpointDialog = new EndpointDialog({
        out: this.out,
        client: this.client,
        env: this.env,
        config: this.config,
        definition: this.definition,
        shouldAskForGenerator: false,
      })
      return await endpointDialog.getDatabase(true)
    }

    return null
  }
  handleMissingArgs(
    requiredArgs: string[],
    providedArgs: string[],
    prefix: string,
  ) {
    const missingArgs = requiredArgs.filter(
      arg => !providedArgs.some(provided => arg === provided),
    )

    throw new Error(
      `If you provide one of the ${prefix}- arguments, you need to provide all of them. The arguments ${missingArgs.join(
        ', ',
      )} are missing.`,
    )
  }
}
