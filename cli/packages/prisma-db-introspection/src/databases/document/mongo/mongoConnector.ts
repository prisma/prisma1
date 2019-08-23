import {
  IDataIterator,
  ObjectTypeIdentifier,
  TypeInfo,
  UnsupportedTypeError,
} from '../documentConnector'
import { DocumentConnector } from '../documentConnectorBase'
import { DatabaseType } from 'prisma-datamodel'
import { MongoClient, Collection, Cursor, AggregationCursor } from 'mongodb'
import * as BSON from 'bson'
import { Data } from '../data'
import { TypeIdentifiers } from 'prisma-datamodel'
import { DatabaseMetadata } from '../../../common/introspectionResult'

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

    if (suggestion.type === ObjectTypeIdentifier) {
      // Special BSON types. DateTime is JS DateTime and handled by base class.
      if (value instanceof BSON.ObjectID) {
        suggestion.type = TypeIdentifiers.id
        suggestion.isRelationCandidate = true
      } else if (value instanceof BSON.Binary) {
        throw new UnsupportedTypeError('Type not supported', 'Binary')
      } else if (value instanceof BSON.BSONRegExp || value instanceof RegExp) {
        throw new UnsupportedTypeError('Type not supported', 'RegExp')
      } else if (value instanceof BSON.Code) {
        throw new UnsupportedTypeError('Type not supported', 'Code')
      } else if (value instanceof BSON.Int32) {
        suggestion.type = TypeIdentifiers.integer
      } else if (value instanceof BSON.Timestamp) {
        throw new UnsupportedTypeError('Type not supported', 'Timestamp')
      } else if (value instanceof BSON.Long) {
        suggestion.type = TypeIdentifiers.long
      } else if (value instanceof BSON.Decimal128) {
        throw new UnsupportedTypeError('Type not supported', 'Decimal128')
      }
    }

    return suggestion
  }

  public async getMetadata(schemaName: string): Promise<DatabaseMetadata> {
    const cols = await this.client.db(schemaName).collections()
    const stats = await this.client.db(schemaName).stats()

    return {
      countOfTables: cols.length,
      sizeInBytes: stats.dataSize,
    }
  }
}
