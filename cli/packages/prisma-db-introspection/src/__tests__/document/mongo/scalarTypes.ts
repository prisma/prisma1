import { MongoConnector } from '../../../databases/document/mongo/mongoConnector'
import { MongoTestEnvironment } from '../../../test-helpers/mongoTestEnvironment'
import { scalars, schemaString } from '../data/mongoTypes'

const env = new MongoTestEnvironment()

describe('Mongo Model Introspector, end to end', () => {
  beforeAll(async () => await env.connect())
  afterAll(async () => await env.disconnect())
  afterEach(async () => await env.clear())

  it('Scalar Types', async () => {
    await env.createCollections({ scalars })

    const connector = new MongoConnector(env.getClient())
    const introspection = await connector.introspect(env.schemaName)
    const schema = await introspection.renderToDatamodelString()

    expect(schema).toEqual(schemaString)
  }, 10000)
})
