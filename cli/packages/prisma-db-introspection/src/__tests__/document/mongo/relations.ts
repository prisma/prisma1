import { MongoConnector } from '../../../databases/document/mongo/mongoConnector'
import { MongoClient } from 'mongodb'
import { MongoTestEnvironment } from '../../../test-helpers/mongoTestEnvironment'
import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import {
  users,
  items,
  assertUserItemModel,
  schemaString,
} from '../data/simpleRelational'

const env = new MongoTestEnvironment()

describe('Mongo Model Introspector', () => {
  beforeAll(async () => await env.connect())
  afterAll(async () => await env.disconnect())
  afterEach(async () => await env.clear())

  it('Should infer a model, embedded types and relations correctly.', async () => {
    await env.createCollection('User', users)
    await env.createCollection('Item', items)

    const connector = new MongoConnector(env.getClient())
    const introspection = await connector.introspect(env.schemaName)
    const sdl = await introspection.getDatamodel()
    const schema = await introspection.renderToDatamodelString()

    assertUserItemModel(sdl.types)
    expect(schema).toEqual(schemaString)
  }, 20000)

  it('Should infer a relation with random sampling correctly.', async () => {
    // Gen large number of items
    const items = [...Array(1000).keys()].map(x => {
      return { _id: `item_${x}`, other: `other_${x}` }
    })
    const others = [...Array(1000).keys()]
      .filter(x => Math.random() >= 0.5)
      .map(x => {
        return { _id: `other_${x}` }
      })

    await env.createCollection('Item', items)
    await env.createCollection('Other', others)

    const connector = new MongoConnector(env.getClient())
    const introspection = await connector.introspect(env.schemaName)
    const sdl = await introspection.getDatamodel()
    const schema = await introspection.renderToDatamodelString()

    const itemType = SdlExpect.type(sdl.types, 'Item', false, false)
    const otherType = SdlExpect.type(sdl.types, 'Other', false, false)

    SdlExpect.field(itemType, 'other', false, false, otherType)
  }, 20000)
})
