import { DocumentConnector } from '../databases/document/documentConnectorBase'
import { IDataIterator, ICollectionDescription } from '../databases/document/documentConnector'
import { Data } from '../databases/document/data'
import { SdlExpect, TypeIdentifiers, DatabaseType } from 'prisma-datamodel'

/**
 * This class mocks a document database in-memory.
 */
export class MockDocumentDataSource extends DocumentConnector<string> {
  private collections: { [name: string]: Data[] }

  constructor(colletions: { [name: string]: Data[] }) {
    super()
    this.collections = colletions
  }

  public async exists(collection: string, id: any): Promise<boolean> {
    return this.collections[collection].some(x => x['_id'] === id)
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.mongo
  }
  public async listSchemas(): Promise<string[]> {
    return ['default']
  }
  public async getInternalCollections(schema: string): Promise<ICollectionDescription<string>[]> {
    return Object.keys(this.collections).map(x => { return { name: x, collection: x }})
  }
  public async getInternalCollection(schema: string, collection: string): Promise<string> {
    return collection
  }
  protected async sampleOne(collection: string): Promise<IDataIterator> {
    return new InMemoryIterator([this.collections[collection][0]])
  }
  protected async sampleMany(collection: string, limit: number): Promise<IDataIterator> {
    return this.sampleAll(collection) // For mocking, we always sample all. 
  }
  protected async sampleAll(collection: string): Promise<IDataIterator> {
    return new InMemoryIterator(this.collections[collection])
  }
} 

export class InMemoryIterator implements IDataIterator {

  private items: Data[]

  constructor(items: Data[]) {
    this.items = [...items]
  }

  public async hasNext(): Promise<boolean> {
    return this.items.length > 0
  }
  public async  next(): Promise<Data> {
    const data = this.items.shift()
    if(data === undefined) {
      throw new Error('Iterator has no more items.')
    }
    return data
  }
  public async close(): Promise<void> { }

}