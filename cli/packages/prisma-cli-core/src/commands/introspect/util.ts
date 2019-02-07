import { Client as PGClient } from 'pg'
import { MongoClient } from 'mongodb'
import { createConnection, Connection } from 'mysql'

import { DatabaseCredentials, EndpointDialog } from '../../utils/EndpointDialog'
import IDatabaseClient from 'prisma-db-introspection/dist/databases/IDatabaseClient'
import { omit } from 'lodash'
import { Connectors } from 'prisma-db-introspection'
import { DatabaseType } from 'prisma-datamodel'
import { IConnector } from 'prisma-db-introspection/dist/common/connector'

function replaceLocalDockerHost(credentials: DatabaseCredentials) {
  if (credentials.host) {
    const replaceMap = {
      'host.docker.internal': 'localhost',
      'docker.for.mac.localhost': 'localhost',
    }
    return {
      ...credentials,
      host: replaceMap[credentials.host] || credentials.host,
    }
  }
  return credentials
}

// const databaseTypeMap = {
//   mongo: DatabaseType.mongo,
//   myqsl: DatabaseType.mysql,
//   postgres: DatabaseType.postgres,
//   sqlite: DatabaseType.sqlite,
// }

// function convertDatabaseType(databaseType: InternalDatabaseType): DatabaseType {
//   return databaseTypeMap[databaseType]
// }

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
}

export async function getConnectedConnectorFromCredentials(
  credentials: DatabaseCredentials,
): Promise<ConnectorAndDisconnect> {
  let client: MongoClient | PGClient | Connection
  let disconnect: () => Promise<void>
  let databaseName

  switch (credentials.type) {
    case DatabaseType.mongo: {
      client = await getConnectedMongoClient(credentials)
      disconnect = () => (client as MongoClient).close()
    }
    case DatabaseType.mysql: {
      client = await getConnectedMysqlClient(credentials)
      disconnect = async () => (client as Connection).end()
    }
    case DatabaseType.postgres: {
      client = await getConnectedPostgresClient(credentials)
      disconnect = () => (client as PGClient).end()
    }
  }

  const connector = Connectors.create(credentials.type, client!)

  return { connector, disconnect: disconnect! }
}

export async function getConnectorWithDatabase(
  connectorData: IntermediateConnectorData,
  endpointDialog: EndpointDialog,
): Promise<ConnectorData> {
  const { connector, disconnect, databaseType, ...result } = connectorData
  let { databaseName } = result

  let schemas: string[]
  try {
    schemas = await connector!.listSchemas()
  } catch (e) {
    throw new Error(`Could not connect to database. ${e.message}`)
  }

  if (databaseName && !schemas.includes(databaseName)) {
    const schemaWord =
      databaseType === DatabaseType.postgres ? 'schema' : 'database'
    throw new Error(
      `The provided ${schemaWord} "${databaseName}" does not exist. The following are available: ${schemas.join(
        ', ',
      )}`,
    )
  }

  if (!databaseName) {
    const schemasWithoutTmpService = schemas.filter(
      s => !s.startsWith('prisma-temporary-introspection-service'),
    )
    databaseName = await endpointDialog.selectSchema(schemasWithoutTmpService)
  }

  return { connector, disconnect, databaseType, databaseName }
}

async function getConnectedMysqlClient(
  credentials: DatabaseCredentials,
): Promise<Connection> {
  const credentialsWithoutSsl = omit<DatabaseCredentials, 'ssl'>(
    replaceLocalDockerHost(credentials),
    ['ssl'],
  )

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

async function getConnectedPostgresClient(
  credentials: DatabaseCredentials,
): Promise<PGClient> {
  const sanitizedCredentials = replaceLocalDockerHost(credentials)
  const client = new PGClient(sanitizedCredentials)
  await client.connect()
  return client
}

function getConnectedMongoClient(
  credentials: DatabaseCredentials,
): Promise<MongoClient> {
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
