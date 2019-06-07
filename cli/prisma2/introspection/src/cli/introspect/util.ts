import chalk from 'chalk'
import { MongoClient } from 'mongodb'
import { Connection, createConnection } from 'mysql'
import { Client as PGClient } from 'pg'
import { DatabaseType } from 'prisma-datamodel'
import { URL } from 'url'
import { Connectors, IConnector } from 'prisma-db-introspection'
import { DatabaseCredentials } from '../types'

function replaceLocalDockerHost(credentials: DatabaseCredentials) {
  if (credentials.host) {
    const replaceMap = {
      'host.docker.internal': 'localhost',
      'docker.for.mac.localhost': 'localhost',
    } as any
    return {
      ...credentials,
      host: replaceMap[credentials.host] || credentials.host,
    }
  }
  return credentials
}

export interface ConnectorAndDisconnect {
  /**
   * The introspection connector instance
   */
  connector: IConnector
  /**
   * Callback to let the client disconnect
   */
  disconnect: () => Promise<void>
}

/**
 * This data is needed to perform the introspection
 */
export interface ConnectorData extends ConnectorAndDisconnect {
  /**
   * The concrete database type used by the Connector that is included in this object
   */
  databaseType: DatabaseType
  /**
   * The database name either directly provided by the user or chosen in the select schema dialog
   */
  databaseName: string
}

/**
 * As we use a separate function to guarantee for the databaseName, it's optional ehre
 */
export interface IntermediateConnectorData extends ConnectorAndDisconnect {
  databaseType: DatabaseType
  databaseName?: string
  interactive: boolean
}

export async function getConnectedConnectorFromCredentials(
  credentials: DatabaseCredentials,
): Promise<ConnectorAndDisconnect> {
  let client: MongoClient | PGClient | Connection
  let disconnect: () => Promise<void>

  switch (credentials.type) {
    case DatabaseType.mongo: {
      client = await getConnectedMongoClient(credentials)
      disconnect = () => (client as MongoClient).close()
      break
    }
    case DatabaseType.mysql: {
      client = await getConnectedMysqlClient(credentials)
      disconnect = async () => (client as Connection).end()
      break
    }
    case DatabaseType.postgres: {
      client = await getConnectedPostgresClient(credentials)
      disconnect = () => (client as PGClient).end()
      break
    }
    case DatabaseType.sqlite:
      throw new Error('sqlite not supported yet')
  }

  const connector = Connectors.create(credentials.type, client!)

  return { connector, disconnect: disconnect! }
}

export async function getDatabaseSchemas(connector: IConnector): Promise<string[]> {
  try {
    return await connector.listSchemas()
  } catch (e) {
    throw new Error(`Could not connect to database. ${e.stack}`)
  }
}

export function assertSchemaExists(databaseName: string, databaseType: DatabaseType, schemas: string[]) {
  if (!schemas.includes(databaseName)) {
    const schemaWord = databaseType === DatabaseType.postgres ? 'schema' : 'database'

    throw new Error(
      `The provided ${schemaWord} "${databaseName}" does not exist. The following are available: ${schemas.join(', ')}`,
    )
  }
}

async function getConnectedMysqlClient(credentials: DatabaseCredentials): Promise<Connection> {
  const { ssl, ...credentialsWithoutSsl } = replaceLocalDockerHost(credentials)
  const client = createConnection(credentialsWithoutSsl)

  await new Promise((resolve, reject) => {
    client.connect(err => {
      if (err) {
        reject(err)
      } else {
        resolve()
      }
    })
  })

  return client
}

export async function getConnectedPostgresClient(credentials: DatabaseCredentials): Promise<PGClient> {
  const sanitizedCredentials = replaceLocalDockerHost(credentials)
  const client = new PGClient(sanitizedCredentials)
  await client.connect()
  return client
}

function getConnectedMongoClient(credentials: DatabaseCredentials): Promise<MongoClient> {
  return new Promise((resolve, reject) => {
    if (!credentials.uri) {
      throw new Error(`Please provide the MongoDB connection string`)
    }

    MongoClient.connect(credentials.uri, { useNewUrlParser: true }, (err, client) => {
      if (err) {
        reject(err)
      } else {
        if (credentials.database) {
          client.db(credentials.database)
        }
        resolve(client)
      }
    })
  })
}

export function sanitizeMongoUri(mongoUri: string) {
  const url = new URL(mongoUri)
  if (url.pathname === '/' || url.pathname.length === 0) {
    url.pathname = 'admin'
  }

  return url.toString()
}

export function populateMongoDatabase({
  uri,
  database,
}: {
  uri: string
  database?: string
}): { uri: string; database: string } {
  const url = new URL(uri)
  if ((url.pathname === '/' || url.pathname.length === 0) && !database) {
    throw new Error(
      `Please provide a Mongo database in your connection string.\nRead more here https://docs.mongodb.com/manual/reference/connection-string/`,
    )
  }

  if (!database) {
    database = url.pathname.slice(1)
  }

  return {
    uri,
    database,
  }
}

export function prettyTime(time: number): string {
  const output = time > 1000 ? (Math.round(time / 100) / 10).toFixed(1) + 's' : time + 'ms'
  return chalk.cyan(output)
}
