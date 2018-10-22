import { Command, flags, Flags } from 'prisma-cli-engine'
import { EndpointDialog, DatabaseCredentials } from '../../utils/EndpointDialog'
import {
  PostgresConnector,
  PrismaDBClient,
  MongoConnector,
} from 'prisma-db-introspection'
import * as path from 'path'
import * as fs from 'fs'
import { prettyTime } from '../../util'
import chalk from 'chalk'
import { Client as PGClient } from 'pg'
import { MongoClient } from 'mongodb'

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
     * Mongo Params
     */
    ['mongo-uri']: flags.string({
      description: 'Mongo connection string',
    }),
    ['mongo-db']: flags.string({
      description: 'Mongo database',
    }),
  }
  static hidden = false
  async run() {
    const { interactive } = this.flags
    const pgHost = this.flags['pg-host']
    const pgPort = this.flags['pg-port']
    const pgUser = this.flags['pg-user']
    const pgPassword = this.flags['pg-password']
    const pgDb = this.flags['pg-db']
    const pgSsl = this.flags['pg-ssl']
    const pgSchema = this.flags['pg-schema']

    const mongoUri = this.flags['mongo-uri']
    const mongoDb = this.flags['mongo-db']

    const endpointDialog = new EndpointDialog({
      out: this.out,
      client: this.client,
      env: this.env,
      config: this.config,
      definition: this.definition,
      shouldAskForGenerator: false,
    })

    let client
    let connector

    /**
     * Handle MongoDB CLI args
     */

    if (mongoUri && !mongoDb) {
      throw new Error(
        `You provided mongo-uri, but not the required option mongo-db`,
      )
    }

    if (mongoDb && !mongoUri) {
      throw new Error(
        `You provided mongo-db, but not the required option mongo-uri`,
      )
    }

    if (mongoDb && mongoUri) {
      client = await this.connectToMongo({
        type: 'mongo',
        uri: mongoUri!,
        database: mongoDb!,
      })
      connector = new MongoConnector(client)
    }

    /**
     * Handle Postgres CLI args
     */

    const requiredPostgresArgs = {
      'pg-host': pgHost,
      'pg-user': pgUser,
      'pg-password': pgPassword,
      'pg-db': pgDb,
      'pg-schema': pgSchema,
    }

    // CONTINUE: Check if either none or all args are present
    const pgArgsEntries = Object.entries(requiredPostgresArgs)
    const notProvidedArgs = pgArgsEntries.filter(([_, value]) => !value)
    if (
      notProvidedArgs.length > 0 &&
      notProvidedArgs.length < pgArgsEntries.length
    ) {
      console.log({
        pgArgsEntriesLength: pgArgsEntries.length,
        notProvidedArgsLength: notProvidedArgs.length,
      })
      throw new Error(
        `If you provide one of the pg- arguments, you need to provide all of them. The arguments ${notProvidedArgs
          .map(([k]) => k)
          .join(', ')} are missing.`,
      )
    }

    if (notProvidedArgs.length === 0) {
      client = new PGClient({
        host: pgHost,
        port: parseInt(pgPort, 10),
        user: pgUser,
        password: pgPassword,
        database: pgDb,
        ssl: pgSsl,
      })
      connector = new PostgresConnector(client)
    }

    if (!interactive) {
      await this.definition.load(this.flags)
      const service = this.definition.service!
      const stage = this.definition.stage!
      const cluster = this.definition.getCluster()
      const workspace = this.definition.getWorkspace()
      this.env.setActiveCluster(cluster!)
      await this.client.initClusterClient(cluster!, service!, stage, workspace!)

      if (await this.hasExecuteRaw()) {
        client = new PrismaDBClient(this.definition)
        connector = new PostgresConnector(client)
      }
    }

    if (!client || !connector) {
      const credentials = await endpointDialog.getDatabase(true)
      if (credentials.type === 'postgres') {
        client = new PGClient(credentials)
        connector = new PostgresConnector(client)
      } else if (credentials.type === 'mongo') {
        client = await this.connectToMongo(credentials)
        connector = new MongoConnector(client)
      }
    }

    let schemas
    const before = Date.now()
    try {
      schemas = await connector.listSchemas()
    } catch (e) {
      throw new Error(`Could not connect to database. ${e.message}`)
    }
    /**
     * Check, if the provided schema exists in the database.
     * This functionality is the same for Mongo and Postgres
     */
    if (schemas && schemas.length > 0) {
      let schema
      if (schemas.length === 1) {
        schema = schemas[0]
      } else if (pgSchema) {
        const exists = schemas.includes(pgSchema)
        if (!exists) {
          throw new Error(
            `The provided Postgres Schema ${pgSchema} does not exist. Choose one of ${schemas.join(
              ', ',
            )}`,
          )
        }
        schema = pgSchema
      } else if (mongoDb) {
        if (!schemas.includes(mongoDb)) {
          throw new Error(
            `The provided Mongo Databse ${mongoDb} does not exist. Choose one of ${schemas.join(
              ', ',
            )}`,
          )
        }

        schema = mongoDb
      } else {
        const schemaName = `${this.definition.service}$${this.definition.stage}`
        const exists = schemas.includes(schemaName)
        schema = exists
          ? schemaName
          : await endpointDialog.selectSchema(
              schemas.filter(
                s => !s.startsWith('prisma-temporary-introspection-service$'),
              ),
            )
      }

      this.out.action.start(`Introspecting schema ${chalk.bold(schema)}`)

      const introspection = await connector.introspect(schema)
      const sdl = await introspection.getDatamodel()
      const numTables = sdl.types.length
      const renderedSdl = introspection.renderer.render(sdl)

      // Mongo has .close, Postgres .end
      if (typeof client.close === 'function') {
        await client.close()
      } else {
        await client.end()
      }
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
      const fileName = `datamodel-${new Date().getTime()}.prisma`
      const fullFileName = path.join(this.config.definitionDir, fileName)
      fs.writeFileSync(fullFileName, renderedSdl)
      this.out.action.stop(prettyTime(Date.now() - before))
      this.out.log(
        `Created datamodel definition based on ${numTables} database tables.`,
      )
      this.out.log(`\
${chalk.bold(
        'Created 1 new file:',
      )}    GraphQL SDL-based datamodel (derived from existing database)

  ${chalk.cyan(fileName)}
`)

      if (!this.definition.definition || !this.definition.definition!.datamodel) {
        await this.definition.load(this.flags)
        this.definition.addDatamodel(fileName)
        this.out.log(
          `Added ${chalk.bold(`datamodel: ${fileName}`)} to prisma.yml`,
        )
      }
    } else {
      throw new Error(`Could not find schema in provided database.`)
    }
  }
  async hasExecuteRaw() {
    try {
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
  connectToMongo(credentials: DatabaseCredentials): Promise<MongoClient> {
    return new Promise((resolve, reject) => {
      if (!credentials.uri) {
        throw new Error(`Please provide the MongoDB connection string`)
      }

      MongoClient.connect(
        credentials.uri,
        { useNewUrlParser: true },
        (err, client) => {
          if (err) {
            reject(err)
          } else {
            if (credentials.database) {
              client.db(credentials.database)
            }
            resolve(client)
          }
        },
      )
    })
  }
}
