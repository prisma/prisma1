import {
  IDataIterator,
  SamplingStrategy,
  ObjectTypeIdentifier,
  TypeInfo,
} from '../documentConnector'
import { DocumentConnector } from '../documentConnectorBase'
import { DatabaseType, ISDL } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from '../documentIntrospectionResult'
import { MongoClient, Collection, Cursor, AggregationCursor } from 'mongodb'
import { ObjectID } from 'bson'
import { Data } from '../data'
import { TypeIdentifiers } from 'prisma-datamodel'

const reservedSchemas = ['admin', 'local']

class MongoCursorIterator implements IDataIterator {
  protected cursor: Cursor<Data> | AggregationCursor<Data>

  public constructor(cursor: Cursor<Data> | AggregationCursor<Data>) {
    this.cursor = cursor
  }

  async hasNext() {
    return this.cursor.hasNext()
  }

  async next() {
    return (await this.cursor.next()) || {}
  }

  async close() {
    await this.cursor.close()
  }
}

export class MongoConnector extends DocumentConnector<Collection<Data>> {
  private client: MongoClient

  constructor(client: MongoClient) {
    super()

    if (!(client instanceof MongoClient)) {
      throw new Error('MongoClient instance needed for initialization.')
    }

    if (!client.isConnected()) {
      throw new Error('Please connect the mongo client first.')
    }

    this.client = client
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.mongo
  }

  async listSchemas(): Promise<string[]> {
    const adminDB = this.client.db().admin()
    const { databases } = await adminDB.listDatabases()
    return databases
      .map(x => x.name)
      .filter(x => reservedSchemas.indexOf(x) < 0)
  }

  public async getInternalCollections(schemaName: string) {
    const db = this.client.db(schemaName)
    const collections = (await db.collections()) as Collection<Data>[]
    return collections.map(collection => {
      return { name: collection.collectionName, collection }
    })
  }

  public async getInternalCollection(
    schemaName: string,
    collectionName: string,
  ) {
    const db = this.client.db(schemaName)
    return await db.collection<Data>(collectionName)
  }

  async sampleOne(collection: Collection): Promise<MongoCursorIterator> {
    const cursor = await collection.find<Data>({}).limit(1)
    return new MongoCursorIterator(cursor)
  }

  async sampleMany(
    collection: Collection,
    limit: number,
  ): Promise<MongoCursorIterator> {
    const cursor = collection.aggregate<Data>([{ $sample: { size: limit } }])
    return new MongoCursorIterator(cursor)
  }

  async sampleAll(collection: Collection): Promise<MongoCursorIterator> {
    const cursor = await collection.find<Data>({})

    return new MongoCursorIterator(cursor)
  }

  async exists(collection: Collection, id: any): Promise<boolean> {
    return collection.find({ _id: id }).hasNext()
  }

  /**
   * Mongo special handling of ObjectID types.
   */
  public inferType(value: any): TypeInfo {
    const suggestion = super.inferType(value)

    if (suggestion.type === ObjectTypeIdentifier && value instanceof ObjectID) {
      suggestion.type = TypeIdentifiers.id
      suggestion.isRelationCandidate = true
    }

    return suggestion
  }
}
