import { DataIterator, SamplingStrategy } from '../documentConnector'
import { DocumentConnector } from '../documentConnectorBase'
import { DatabaseType, ISDL } from "prisma-datamodel"
import { DocumentIntrospectionResult } from "../documentIntrospectionResult"
import { MongoClient, Collection, Cursor } from 'mongodb'
import { Data } from '../data'

const reservedSchemas = ['admin', 'local']


class MongoCursorIterator implements DataIterator {
  private cursor: Cursor<Data>

  public constructor(cursor: Cursor<Data>) {
    this.cursor = cursor
  }

  async hasNext() {
    return this.cursor.hasNext()
  }

  async next() {
    return (await this.cursor.next()) || {}
  }
}


export class MongoConnector extends DocumentConnector<Collection<Data>>{
  private client: MongoClient
  
  constructor(client: MongoClient) {
    super()

    if(!client.isConnected()) {
      throw new Error('Please connect the mongo client first.')
    }

    this.client = client
  }

  getDatabaseType(): DatabaseType {
    return DatabaseType.mongo
  }

  async listSchemas(): Promise<string[]> {
    const adminDB = this.client.db().admin()
    const { databases } = await adminDB.listDatabases()
    return databases.map(x => x.name).filter(x => reservedSchemas.indexOf(x) < 0)
  }
  
  public async getInternalCollections(schemaName: string) {
    const db = this.client.db(schemaName)

    const collections = (await db.collections()) as Collection<Data>[]
    return collections.map(collection => { return { name: collection.collectionName, collection }})
  }

  
  public async getInternalCollection(schemaName: string, collectionName: string) {
    const db = this.client.db(schemaName)

    return await db.collection<Data>(collectionName)
  }
  
  // TODO: Lift to strategy
  async sampleOne(collection: Collection) : Promise<MongoCursorIterator> {
    const cursor = await collection.find<Data>({}).limit(1)
    return new MongoCursorIterator(cursor)
  }

  async sampleMany(collection: Collection, limit: number) : Promise<MongoCursorIterator> {
    throw new Error("Not implemented")/*
    const count = await collection.count({})
    if(count < limit) {
      return await this.sampleAll(collection)
    } else {
      let cursor = collection.find<Data>({})
      // TODO: This sampling is biased, but should do the job. 
      for(const skip of this.generateSamples(limit, count)) {
        cursor.skip(skip)
        const data = (await cursor.next())
        if(data === null) {
          throw new Error('Sample many error, cursor returned null for collection: ' + collection.collectionName)
        }
        yield data
      }
    }*/
  }

  /** 
  * Gets n random numbers chich sum up to sum.
  */
  private *generateSamples(n: number, sum: number) {
    for(let i = 0; i < n; i++) {
      yield Math.floor(Math.random() * sum / n)
    }
  }

  async sampleAll(collection: Collection) : Promise<MongoCursorIterator> {
    const cursor = await collection.find<Data>({})

    return new MongoCursorIterator(cursor);
  }

  async exists(collection: Collection, id: any): Promise<boolean> {
    return collection.find({ '_id': id }).hasNext()
  }

}