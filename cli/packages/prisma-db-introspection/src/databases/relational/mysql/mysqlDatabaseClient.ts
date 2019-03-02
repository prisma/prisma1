import IDatabaseClient from '../../IDatabaseClient'
import { Connection } from 'mysql2'

export default class MySqlDatabaseClient implements IDatabaseClient {
  private client: Connection

  constructor(client: Connection) {
    this.client = client
  }

  public query(query: string, variables: any[]): Promise<any[]> {
    return new Promise<any[]>((resolve, reject) => {
      this.client.query(query, variables, (err, res) => {
        if (err) {
          reject(err)
        } else {
          resolve(res)
        }
      })
    })
  }
}
