import {
  getConnectedPostgresClient,
  getConnectedConnectorFromCredentials,
  getDatabaseSchemas,
} from './cli/introspect/util'
import { DatabaseType } from 'prisma-datamodel'

async function run() {
  const credentials = {
    host: '116.202.29.54',
    port: 5434,
    password: 'xeeceevieL1aicoithohGi2KahbaedohGaib',
    user: 'timsu',
    type: DatabaseType.postgres,
  }
  const { connector, disconnect } = await getConnectedConnectorFromCredentials(credentials)

  const schemas = await getDatabaseSchemas(connector)

  console.log(schemas)

  const introspection = await connector.introspect(schemas[0])
  await disconnect()
}

run().catch(console.error)
