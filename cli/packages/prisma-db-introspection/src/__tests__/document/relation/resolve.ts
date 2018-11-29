import { SdlExpect, TypeIdentifiers } from 'prisma-datamodel'
import { ModelMerger, ModelSampler } from '../../../databases/document/modelSampler'
import { ObjectID } from 'bson'
import { IDataExists } from '../../../databases/document/documentConnector';
import { Data } from '../../../databases/document/data';
import { RelationResolver } from '../../../databases/document/relationResolver';


class MockDataSource implements IDataExists<string> {
  private collections: { [name: string]: Data[] }

  constructor(colletions: { [name: string]: Data[] }) {
    this.collections = colletions
  }

  async exists(collection: string, id: any): Promise<boolean> {
    return this.collections[collection].some(x => x['_id'] === id)
  }

} 

/**
 * Checks if model sampling and inferring marks potential relation field correctly.
 * 
 * Depends on the correctness of all model tests. On multiple errors, fix model tests first. 
 */
describe('Document relation inferring, should connect correctly', () => {
  it('Should mark potential relation fields correctly.', () => {
    const users = [{
      _id: 'user1@prisma.com',
      firstName: 'Charlotte',
      orders: [{
        count: 5,
        item: 'Fridge'
      }, {
        count: 1,
        item: 'Espresso'
      }]
    }, {
      _id: 'user2@prisma.com',
      firstName: 'Dolores',
      orders: []
    }, {
      _id: 'user3@prisma.com',
      firstName: 'Humbert',
      orders: [{
        count: 2,
        item: 'Laptop'
      }]
    }]

    const items = [{
      _id: 'Fridge',
      cost: 200
    }, {
      _id: 'Laptop',
      cost: 2500
    }, {
      _id: 'Espresso',
      cost: 1
    }]

    const userMerger = new ModelMerger('User')
    users.forEach(x => userMerger.analyze(x))
    const userResult = userMerger.getType()

    const itemMerger = new ModelMerger('Item')
    users.forEach(x => itemMerger.analyze(x))
    const itemResult = itemMerger.getType()
    
    const allTypes = [userResult.type, ...userResult.embedded, itemResult.type, ...itemResult.embedded]

    const mockDataSource = new MockDataSource({ User: users, Items: items })

    const userResolver = new RelationResolver()

    // TODO: Document connector iface?
    //await userResolver.resolve(allTypes, )
  })
})