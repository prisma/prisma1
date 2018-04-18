import { Table, Column, TypeIdentifier } from '../types/common'
import * as _ from 'lodash'
import { Client } from 'pg'

export class PostgresConnector {
  client

  constructor(connectionString: string) {
    this.client = new Client({ connectionString: connectionString })
  }

  async queryRelations(schemaName: string) {
    const res = await this.client.query(
      `SELECT
    tc.table_schema, tc.constraint_name, tc.table_name, kcu.column_name, 
    ccu.table_schema AS foreign_table_schema,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM 
    information_schema.table_constraints AS tc 
    JOIN information_schema.key_column_usage AS kcu
      ON tc.constraint_name = kcu.constraint_name
    JOIN information_schema.constraint_column_usage AS ccu
      ON ccu.constraint_name = tc.constraint_name
WHERE constraint_type = 'FOREIGN KEY' AND tc.table_schema = $1::text;`,
      [schemaName.toLowerCase()]
    )

    return res.rows
  }

  async queryTables(schemaName: string) {
    const res = await this.client.query(
      `select table_name, column_name, is_nullable, data_type, udt_name from information_schema.columns where table_schema = $1::text`,
      [schemaName.toLowerCase()]
    )

    const tables = _.groupBy(res.rows, 'table_name')

    return tables
  }

  async querySchemas() {
    const res = await this.client.query(
      `select schema_name from information_schema.schemata WHERE schema_name NOT LIKE 'pg_%' AND schema_name NOT LIKE 'information_schema';`
    )

    return res.rows.map(x => x.schema_name)
  }

  extractRelation(table, column, relations) {
    const candidate = relations.find(
      relation =>
        relation.table_name === table && relation.column_name === column
    )

    if (candidate) {
      return {
        table: candidate.foreign_table_name,
      }
    } else {
      return null
    }
  }

  async listSchemas(): Promise<string[]> {
    await this.client.connect()

    const schemas = await this.querySchemas()
    await this.client.end()

    return schemas
  }

  async listTables(schemaName): Promise<Table[]> {
    await this.client.connect()

    const relations = await this.queryRelations(schemaName)
    const tables = await this.queryTables(schemaName)

    await this.client.end()

    const withColumns = _.map(tables, (originalColumns, key) => {
      const columns = _.map(originalColumns, column => {
        const { typeIdentifier, comment, error } = this.toTypeIdentifier(
          column.data_type
        )
        const relation = this.extractRelation(
          column.table_name,
          column.column_name,
          relations
        )

        return {
          name: column.column_name,
          type: column.data_type,
          typeIdentifier,
          comment,
          relation: relation,
          nullable: column.is_nullable === 'YES',
        } as Column
      }).filter(x => x != null) as Column[]

      const sortedColumns = _.sortBy(columns, x => x.name)

      return {
        name: key,
        columns: sortedColumns,
      }
    })

    const sortedTables = _.sortBy(withColumns, x => x.name)

    return sortedTables
  }

  toTypeIdentifier(
    type: string
  ): {
    typeIdentifier: TypeIdentifier | null
    comment: string | null
    error: string | null
  } {
    if (type === 'character') {
      return { typeIdentifier: 'String', comment: null, error: null }
    }
    if (type === 'character varying') {
      return { typeIdentifier: 'String', comment: null, error: null }
    }
    if (type === 'text') {
      return { typeIdentifier: 'String', comment: null, error: null }
    }
    if (type === 'smallint') {
      return { typeIdentifier: 'Int', comment: null, error: null }
    }
    if (type === 'integer') {
      return { typeIdentifier: 'Int', comment: null, error: null }
    }
    if (type === 'bigint') {
      return { typeIdentifier: 'Int', comment: null, error: null }
    }
    if (type === 'real') {
      return { typeIdentifier: 'Float', comment: null, error: null }
    }
    if (type === 'double precision') {
      return { typeIdentifier: 'Float', comment: null, error: null }
    }
    if (type === 'numeric') {
      return { typeIdentifier: 'Float', comment: null, error: null }
    }
    if (type === 'boolean') {
      return { typeIdentifier: 'Boolean', comment: null, error: null }
    }
    if (type === 'timestamp without time zone') {
      return { typeIdentifier: 'DateTime', comment: null, error: null }
    }
    if (type === 'timestamp') {
      return { typeIdentifier: 'DateTime', comment: null, error: null }
    }
    if (type === 'json') {
      return { typeIdentifier: 'Json', comment: null, error: null }
    }

    return {
      typeIdentifier: null,
      comment: null,
      error: `Not able to handle type '${type}'`,
    }
  }
}
