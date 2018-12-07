import { SdlExpect, TypeIdentifiers, DatabaseType } from 'prisma-datamodel'
import { ModelMerger, ModelSampler } from '../../../databases/document/modelSampler'
import { Data } from '../../../databases/document/data'
import { RelationResolver } from '../../../databases/document/relationResolver'
import { collections, schemaString } from '../data/webshop'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource'

describe('Document relation inferring, end to end examples', () => {
  it('Webshop', async () => {
    const mockDataSource = new MockDocumentDataSource(collections)
    const result = await mockDataSource.introspect('default')
    const sdl = await result.getDatamodel()

    expect(await result.renderToDatamodelString()).toBe(schemaString)
  })
})