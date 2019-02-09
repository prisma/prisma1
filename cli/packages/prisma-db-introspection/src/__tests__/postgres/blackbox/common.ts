import { connectionDetails } from './connectionDetails'
import { Client } from 'pg'
import Connectors from '../../../connectors'
import { DatabaseType } from 'prisma-datamodel'

export default async function testSchema(
  schemaSql: string,
  schemaName: string = 'DatabaseIntrospector',
  createSchema: boolean = true,
) {
  expect(await createAndIntrospect(schemaSql, schemaName, createSchema)).toMatchSnapshot()
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

  const res = (await Connectors.create(DatabaseType.postgres, client).introspect(
    schemaName,
  )).renderToNormalizedDatamodelString()

  await client.end()

  return res
}
