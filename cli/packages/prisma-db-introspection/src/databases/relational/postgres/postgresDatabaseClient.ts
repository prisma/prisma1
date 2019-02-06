import IDatabaseClient from "../../IDatabaseClient"
import { Client } from "pg"

export default class PostgresDatabaseClient implements IDatabaseClient {
  private client: Client

  constructor(client: Client) {
    this.client = client
  }

  public async query(query: string, variables: any[]): Promise<any[]> {
    return (await this.client.query(query, variables)).rows
  }
  
}