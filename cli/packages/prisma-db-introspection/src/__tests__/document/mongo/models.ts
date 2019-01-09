import { MongoConnector } from '../../../databases/document/mongo/mongoConnector'
import { MongoClient } from 'mongodb'
import { MongoTestEnvironment } from '../../../test-helpers/mongoTestEnvironment'
import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'

const env = new MongoTestEnvironment()

describe("Mongo Model Introspector", () => {

  beforeAll(async () => await env.connect())
  afterAll(async () => await env.disconnect())
  afterEach(async () => await env.clear())

  it("Should infer a model and basic scalar and array types correctly.", async () => {
    await env.createCollection('Movie', [{ 
      name: 'Titanic',
      genre: 'Science Fiction',
      year: 1991,
      rating: 9.5,
      hasLeonardo: true,
      roles: ['Rose', 'Jake']
     }])

    const connector = new MongoConnector(env.getClient())
    const introspection = await connector.introspect(env.schemaName)
    const sdl = await introspection.getDatamodel()
    const types = sdl.types

    expect(types).toHaveLength(1)
    
    const movieType = SdlExpect.type(types, 'Movie')

    expect(movieType.fields).toHaveLength(7)
    
    SdlExpect.field(movieType, '_id', true, false, TypeIdentifiers.id, true)
    SdlExpect.field(movieType, 'name', false, false, TypeIdentifiers.string)
    SdlExpect.field(movieType, 'genre', false, false, TypeIdentifiers.string)
    SdlExpect.field(movieType, 'year', false, false, TypeIdentifiers.integer)
    SdlExpect.field(movieType, 'rating', false, false, TypeIdentifiers.float)
    SdlExpect.field(movieType, 'hasLeonardo', false, false, TypeIdentifiers.boolean)
    SdlExpect.field(movieType, 'roles', false, true, TypeIdentifiers.string)
  }, 10000)
})