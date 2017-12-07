import { Client } from 'graphcool-cli-engine'
import { buildClientSchema, printSchema } from 'graphql'

export async function fetchAndPrintSchema(
  client: Client,
  serviceName: string,
  stageName: string,
): Promise<string> {
  const introspection = await client.introspect(serviceName, stageName)
  const schema = buildClientSchema(introspection)
  return printSchema(schema)
}
