import { MongoClient } from "mongodb";
import { Data } from "../databases/document/data";
import { IConnector } from "../common/connector";
import { IDocumentConnector } from "../databases/document/documentConnector";


export interface IDocumentTestEnvironment {
  schemaName: string
  connect(): Promise<void>
  disconnect(): Promise<void>
  clear(): Promise<void>
  createCollection(name: string, document: Data[]) : Promise<void>
  createCollections(collections: { [name: string]: Data[] }) : Promise<void>
}

export class MongoTestEnvironment implements IDocumentTestEnvironment {
  private uri: string
  private schema: string
  private client: MongoClient
  public readonly schemaName: string

  constructor(uri?: string, schema?: string) {
    this.schema = schema || process.env.TEST_MONGO_SCHEMA || ''
    this.uri = uri || process.env.TEST_MONGO_URI || ''

    this.schemaName = this.schema
    this.client = new MongoClient(this.uri)
  }

  public async connect() {
    await this.client.connect()
  }

  public getClient() {
    return this.client
  }

  public getSchema() {
    return this.client.db(this.schema)
  }

  public async clear() {
    await this.getSchema().dropDatabase()
  }

  public async createCollection(name: string, documents: Data[]) {
    const db = await this.getSchema()
    const collection = await db.createCollection<Data>(name)
    await collection.insertMany(documents)
  }

  public async createCollections(collections: { [name: string]: Data[] }) {
    for(const name of Object.keys(collections)) {
      await this.createCollection(name, collections[name]);
    }
  }

  public async disconnect() {
    await this.client.close()
  }
}