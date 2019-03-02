import { SdlExpect, TypeIdentifiers, DatabaseType } from 'prisma-datamodel'
import {
  ModelMerger,
  ModelSampler,
} from '../../../databases/document/modelSampler'
import { Data } from '../../../databases/document/data'
import { RelationResolver } from '../../../databases/document/relationResolver'
import {
  users,
  items,
  assertUserItemModel,
  schemaString,
} from '../data/simpleRelational'
import { MockDocumentDataSource } from '../../../test-helpers/mockDataSource'

/**
 * Checks if model sampling and inferring marks potential relation field correctly.
 *
 * Depends on the correctness of all model tests. On multiple errors, fix model tests first.
 */
describe('Document relation inferring, should connect correctly', () => {
  it('Should associate relation fields correctly.', async () => {
    const mockDataSource = new MockDocumentDataSource({
      User: users,
      Item: items,
    })

    const userMerger = new ModelMerger('User', false, mockDataSource)
    users.forEach(x => userMerger.analyze(x))
    const userResult = userMerger.getType()

    const itemMerger = new ModelMerger('Item', false, mockDataSource)
    items.forEach(x => itemMerger.analyze(x))
    const itemResult = itemMerger.getType()

    const allTypes = [
      userResult.type,
      ...userResult.embedded,
      itemResult.type,
      ...itemResult.embedded,
    ]

    const resolver = new RelationResolver<string>()

    await resolver.resolve(allTypes, mockDataSource, 'default')

    assertUserItemModel(allTypes)
  })

  it('Should associate relation fields correctly, end to end', async () => {
    const mockDataSource = new MockDocumentDataSource({
      User: users,
      Item: items,
    })
    const result = await mockDataSource.introspect('default')
    const sdl = await result.getDatamodel()

    assertUserItemModel(sdl.types)
    expect(await result.renderToDatamodelString()).toEqual(schemaString)
  })
})
