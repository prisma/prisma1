import { connectionDetails } from './connectionDetails'
import * as mysql from 'mysql'
import Connectors from '../../../connectors'
import { DatabaseType, DefaultRenderer } from 'prisma-datamodel'
import MysqlClient from '../../../databases/relational/mysql/mysqlDatabaseClient'

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
  const dbClient = mysql.createConnection(connectionDetails)
  await dbClient.connect()
  const client = new MysqlClient(dbClient)
  await client.query(
    `DROP DATABASE IF EXISTS \`schema-generator@${name}\`;`,
    [],
  )

  await client.query(`CREATE DATABASE \`schema-generator@${name}\`;`, [])
  await client.query(`USE \`schema-generator@${name}\`;`, [])

  await client.query(schemaSql, [])

  const dml = (await Connectors.create(DatabaseType.mysql, client).introspect(
    `schema-generator@${name}`,
  )).getNormalizedDatamodel()

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
