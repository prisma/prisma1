import { Command, flags, Flags } from 'prisma-cli-engine'
import { EndpointDialog, DatabaseCredentials } from '../../utils/EndpointDialog'
import { PrismaDBClient, Connectors } from 'prisma-db-introspection'
import * as path from 'path'
import * as fs from 'fs'
import { prettyTime } from '../../utils/util'
import chalk from 'chalk'
import {
  DefaultParser,
  DatabaseType,
  DefaultRenderer,
  ISDL,
} from 'prisma-datamodel'
import {
  getConnectedConnectorFromCredentials,
  ConnectorData,
  getConnectorWithDatabase,
  IntermediateConnectorData,
  populateMongoDatabase,
  sanitizeMongoUri,
} from './util'

export default class IntrospectCommand extends Command {
  static topic = 'introspect'
  static description = 'Introspect database schema(s) of service'
  static printVersionSyncWarning = true
  static flags: Flags = {
    interactive: flags.boolean({
      char: 'i',
      description: 'Interactive mode',
    }),

    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
    ['project']: flags.string({
      description: 'Path to Prisma definition file',
      char: 'p',
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

    ['sdl']: flags.boolean({
      description:
        'Omit any CLI output and just print the resulting datamodel. Requires an existing Prisma project with executeRaw. Useful for scripting',
    }),
  }
  static hidden = false
  async run() {
    const { sdl } = this.flags
    /**
     * Get connector and connect to database
     */
    const connectorData = await this.getConnectorWithDatabase()

    /**
     * Introspect the database
     */

    const before = Date.now()
    if (!sdl) {
      this.out.action.start(
        `Introspecting database ${chalk.bold(connectorData.databaseName)}`,
      )
    }
    const {
      sdl: newDatamodelSdl,
      numTables,
      referenceDatamodelExists,
    } = await this.introspect(connectorData)
    if (!sdl) {
      this.out.action.stop(prettyTime(Date.now() - before))
    }

    if (!sdl) {
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

      if (
        this.definition.definition &&
        !this.definition.definition!.datamodel
      ) {
        await this.definition.load(this.flags)
        this.definition.addDatamodel(fileName)
        this.out.log(
          `Added ${chalk.bold(`datamodel: ${fileName}`)} to prisma.yml`,
        )
      }
    } else {
      this.out.log(newDatamodelSdl)
    }
  }

  getExistingDatamodel(databaseType: DatabaseType): ISDL | null {
    if (this.definition.typesString) {
      const ParserInstance = DefaultParser.create(databaseType!)
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
      : await introspection.getNormalizedDatamodel()

    const renderer = DefaultRenderer.create(introspection.databaseType, true)
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
    const fileName = `datamodel-${new Date().getTime()}.prisma`
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
      const cluster = await this.definition.getCluster()
      this.env.setActiveCluster(cluster!)
      await this.client.initClusterClient(cluster!, service!, stage, workspace!)
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
   * This method makes sure, that a concrete database to introspect is selected
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
    let credentials = this.getCredentialsByFlags()
    let interactive = false

    if (!credentials) {
      credentials = await this.getCredentialsInteractively(hasExecuteRaw)
      interactive = true

      if (!hasExecuteRaw && this.flags.sdl) {
        throw new Error(
          `When using the --sdl flag, either executeRaw or credentials must be available`,
        )
      }
    }

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
        interactive,
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
      interactive: false,
    }
  }

  getCredentialsByFlags(): DatabaseCredentials | null {
    const requiredPostgresFlags = ['pg-host', 'pg-user', 'pg-password', 'pg-db']
    const requiredMysqlFlags = ['mysql-host', 'mysql-user', 'mysql-password']

    const flags = this.getSanitizedFlags()
    const flagsKeys = Object.keys(flags)

    const mysqlFlags = flagsKeys.filter(f => requiredMysqlFlags.includes(f))
    const postgresFlags = flagsKeys.filter(f =>
      requiredPostgresFlags.includes(f),
    )

    if (mysqlFlags.length > 0 && postgresFlags.length > 0) {
      throw new Error(
        `You can't provide both MySQL and Postgres connection flags. Please provide either of both.`,
      )
    }

    if (
      mysqlFlags.length > 0 &&
      mysqlFlags.length < requiredMysqlFlags.length
    ) {
      this.handleMissingArgs(requiredMysqlFlags, mysqlFlags, 'mysql')
    }

    if (
      postgresFlags.length > 0 &&
      postgresFlags.length < requiredPostgresFlags.length
    ) {
      this.handleMissingArgs(requiredPostgresFlags, postgresFlags, 'pg')
    }

    if (mysqlFlags.length >= requiredMysqlFlags.length) {
      return {
        host: flags['mysql-host'],
        port: parseInt(flags['mysql-port'], 10),
        user: flags['mysql-user'],
        password: flags['mysql-password'],
        schema: flags['mysql-db'],
        type: DatabaseType.mysql,
      }
    }

    if (postgresFlags.length >= requiredPostgresFlags.length) {
      return {
        host: flags['pg-host'],
        user: flags['pg-user'],
        password: flags['pg-password'],
        database: flags['pg-db'],
        port: parseInt(flags['pg-port'], 10),
        schema: flags['pg-schema'], // this is optional and can be undefined
        type: DatabaseType.postgres,
      }
    }

    if (flags['mongo-uri']) {
      const uri = flags['mongo-uri']
      const database = flags['mongo-db'] // this is optional and can be undefined
      const credentials = populateMongoDatabase({ uri, database })
      return {
        uri: sanitizeMongoUri(credentials.uri),
        schema: credentials.database,
        type: DatabaseType.mongo,
      }
    }

    return null
  }

  async getCredentialsInteractively(
    hasExecuteRaw: boolean,
  ): Promise<DatabaseCredentials | null> {
    if (this.flags.interactive || !hasExecuteRaw) {
      const endpointDialog = new EndpointDialog({
        out: this.out,
        client: this.client,
        env: this.env,
        config: this.config,
        definition: this.definition,
        shouldAskForGenerator: false,
      })
      return endpointDialog.getDatabase(true)
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
