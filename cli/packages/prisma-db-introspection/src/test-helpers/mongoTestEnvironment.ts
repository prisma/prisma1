import { MongoClient } from "mongodb";
import { Data } from "../databases/document/data";


// TODO: Find good interface for all test environments
export class MongoTestEnvironment {
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

  public async disconnect() {
    await this.client.close()
  }
}