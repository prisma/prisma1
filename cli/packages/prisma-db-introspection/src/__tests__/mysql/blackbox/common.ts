import { connectionDetails } from './connectionDetails'
import * as mysql from 'mysql'
import Connectors from '../../../connectors'
import { DatabaseType, DefaultRenderer } from 'prisma-datamodel'
import MysqlClient from '../../../databases/relational/mysql/mysqlDatabaseClient'
import { DatabaseMetadata } from '../../../common/introspectionResult'

export default async function testSchema(
  schemaSql: string,
  schemaName: string = 'DatabaseIntrospector',
  createSchema: boolean = true,
) {
  checkSnapshot(await createAndIntrospect(schemaSql, schemaName, createSchema))
}

export async function createAndIntrospect(
  schemaSql: string,
  schemaName: string = 'DatabaseIntrospector',
  createSchema: boolean = true,
) {
  const internalSchemaName = `schema-generator@${schemaName}`

  const dbClient = mysql.createConnection(connectionDetails)
  await dbClient.connect()
  const client = new MysqlClient(dbClient)
  await client.query(`DROP DATABASE IF EXISTS \`${internalSchemaName}\`;`, [])
  await client.query(`CREATE DATABASE \`${internalSchemaName}\`;`, [])
  await client.query(`USE \`${internalSchemaName}\`;`, [])
  await client.query(schemaSql, [])
  const connector = Connectors.create(DatabaseType.mysql, client)
  const dml = (await connector.introspect(
    internalSchemaName,
  )).getNormalizedDatamodel()

  const metadata = await connector.getMetadata(internalSchemaName)

  // Never matches, as size in bytes is always different.
  // console.log(metadata)

  // V2 rendering
  const renderer = DefaultRenderer.create(DatabaseType.postgres, true)
  const rendered = renderer.render(dml)

  // V1 rendering
  const legacyRenderer = DefaultRenderer.create(DatabaseType.postgres, false)
  const legacyRendered = legacyRenderer.render(dml)

  await dbClient.end()

  return { v1: legacyRendered, v2: rendered }
}

export function checkSnapshot(res: { v1: string; v2: string }) {
  expect(res.v1).toMatchSnapshot()
  expect(res.v2).toMatchSnapshot()
}
