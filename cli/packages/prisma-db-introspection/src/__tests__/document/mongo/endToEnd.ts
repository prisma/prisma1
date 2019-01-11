import { MongoConnector } from '../../../databases/document/mongo/mongoConnector'
import { MongoTestEnvironment } from '../../../test-helpers/mongoTestEnvironment'
import { collections, schemaString } from '../data/webshop'

const env = new MongoTestEnvironment()

describe('Mongo Model Introspector, end to end', () => {
  beforeAll(async () => await env.connect())
  afterAll(async () => await env.disconnect())
  afterEach(async () => await env.clear())

  it('Webshop', async () => {
    await env.createCollections(collections)

    const connector = new MongoConnector(env.getClient())
    const introspection = await connector.introspect(env.schemaName)
    const sdl = await introspection.getDatamodel()
    const schema = await introspection.renderToDatamodelString()

    expect(schema).toEqual(schemaString)
  }, 20000)
})
