import { connectionDetails } from './connectionDetails'
import { Client } from 'pg'
import Connectors from '../../../connectors'
import { DatabaseType, DefaultRenderer } from 'prisma-datamodel'

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
  const client = new Client(connectionDetails)
  await client.connect()
  await client.query(`DROP SCHEMA IF EXISTS "${schemaName}" cascade;`)

  if (createSchema) {
    await client.query(`CREATE SCHEMA "${schemaName}";`)
    await client.query(`SET search_path TO "${schemaName}";`)
  }
  await client.query(schemaSql)

  const connector = Connectors.create(DatabaseType.postgres, client)
  const dml = (await connector.introspect(schemaName)).getNormalizedDatamodel()

  const metadata = await connector.getMetadata(schemaName)

  // Snapshot would never match
  // console.log(metadata)

  // V2 rendering
  const renderer = DefaultRenderer.create(DatabaseType.postgres, true)
  const rendered = renderer.render(dml)

  // V1 rendering
  const legacyRenderer = DefaultRenderer.create(DatabaseType.postgres, false)
  const legacyRendered = legacyRenderer.render(dml)

  await client.end()

  return { v1: legacyRendered, v2: rendered }
}

export function checkSnapshot(res: { v1: string; v2: string }) {
  expect(res.v1).toMatchSnapshot()
  expect(res.v2).toMatchSnapshot()
}
