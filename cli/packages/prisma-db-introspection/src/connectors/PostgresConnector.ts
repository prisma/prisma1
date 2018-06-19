import {
  Table,
  Column,
  TypeIdentifier,
  PrimaryKey,
  PostgresConnectionDetails,
} from '../types/common'
import * as _ from 'lodash'
import { Client } from 'pg'

export class PostgresConnector {
  client
  connectionPromise

  constructor(connectionDetails: PostgresConnectionDetails) {
    this.client = new Client(connectionDetails)
    this.connectionPromise = this.client.connect()
    // auto disconnect. end waits for queries to succeed
    setTimeout(() => {
      this.client.end()
    }, 3000)
  }

  async queryRelations(schemaName: string) {
    const res = await this.client.query(
      `
SELECT
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

  // Queries all columns of all tables in given schema and returns them grouped by table_name
  async queryTables(schemaName: string) {
    //, table_name, column_name, is_nullable, data_type, udt_name, column_default
    const res = await this.client.query(
      `
      SELECT *, (SELECT EXISTS(
        SELECT *
        FROM information_schema.table_constraints AS tc 
        JOIN information_schema.key_column_usage AS kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE constraint_type = 'UNIQUE' 
        AND tc.table_schema = $1::text
        AND tc.table_name = c.table_name
        AND kcu.column_name = c.column_name)) as is_unique
      FROM  information_schema.columns c
      WHERE table_schema = $1::text
      `,
      [schemaName.toLowerCase()]
    )

    return _.groupBy(res.rows, 'table_name')
  }

  async queryPrimaryKeys(schemaName: string): Promise<PrimaryKey[]> {
    return this.client.query(
      `
      SELECT tc.table_name, kc.column_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.key_column_usage kc 
        ON kc.table_name = tc.table_name 
        AND kc.table_schema = tc.table_schema
        AND kc.constraint_name = tc.constraint_name
      WHERE tc.constraint_type = 'PRIMARY KEY'
      AND tc.table_schema = $1::text
      AND kc.ordinal_position IS NOT NULL
      ORDER BY tc.table_name, kc.position_in_unique_constraint;
      `,
      [schemaName.toLowerCase()]
    ).then(keys => {
      const grouped = _.groupBy(keys.rows, 'table_name')
      return _.map(grouped, (pks, key) => {
        return {
          tableName: key,
          fields: pks.map(x => x.column_name)
        } as PrimaryKey
      })
    })
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
    await this.connectionPromise
    return await this.querySchemas()
  }

  async listTables(schemaName: string): Promise<Table[]> {
    await this.connectionPromise

    const [relations, tables, primaryKeys] = await Promise.all([
      this.queryRelations(schemaName),
      this.queryTables(schemaName),
      this.queryPrimaryKeys(schemaName)
    ])

    const withColumns = _.map(tables, (originalColumns, key) => {
      const tablePrimaryKey = primaryKeys.find(pk => pk.tableName === key) || null
      const columns = _.map(originalColumns, column => {

        // Ignore compound keys for now
        const isPk = Boolean(tablePrimaryKey
          && tablePrimaryKey.fields.length == 1
          && Boolean(tablePrimaryKey.fields.includes(column.column_name))
        )

        const { typeIdentifier, comment, error } = this.toTypeIdentifier(
          column.data_type,
          column.column_name,
          isPk
        )

        const relation = this.extractRelation(
          column.table_name,
          column.column_name,
          relations
        )

        const col = {
          isUnique: column.is_unique || isPk,
          isPrimaryKey: isPk,
          defaultValue: this.parseDefaultValue(column.column_default),
          name: column.column_name,
          type: column.data_type,
          typeIdentifier,
          comment,
          relation: relation,
          nullable: column.is_nullable === 'YES',
        } as Column

        return col
      }).filter(x => x != null) as Column[]

      // todo: relation table if 2 foreign keys. Also: No id field? + no other column that has NOT NULL and no default
      const sortedColumns = _.sortBy(columns.filter(c => !c.isPrimaryKey), x => x.name)
      const primaryKeyCol = columns.find(c => c.isPrimaryKey)
      if (primaryKeyCol) {
        sortedColumns.unshift(primaryKeyCol)
      }

      const isJunctionTable =
        sortedColumns.length === 2 &&
        sortedColumns.every(x => x.relation != null)

      return {
        name: key,
        columns: sortedColumns,
        isJunctionTable: isJunctionTable,
      }
    })

    return _.sortBy(withColumns, x => x.name)
  }

  parseDefaultValue(string) {
    if (string == null) {
      return null
    }

    if (string.includes(`nextval('`)) {
      return '[AUTO INCREMENT]'
    }

    if (string.includes('now()') || string.includes("'now'::text")) {
      return null
    }

    if (string.includes('::')) {
      const candidate = string.split('::')[0]
      const withoutSuffix = candidate.endsWith(`'`)
        ? candidate.substring(0, candidate.length - 1)
        : candidate
      const withoutPrefix = withoutSuffix.startsWith(`'`)
        ? withoutSuffix.substring(1, withoutSuffix.length)
        : withoutSuffix

      if (withoutPrefix === "NULL") {
        return null
      }

      return withoutPrefix
    }

    return string
  }

  toTypeIdentifier(
    type: string,
    field: string,
    isPrimaryKey: boolean
  ): {
      typeIdentifier: TypeIdentifier | null
      comment: string | null
      error: string | null
    } {
    if (
      isPrimaryKey &&
      (type === 'character' ||
        type === 'character varying' ||
        type === 'text' ||
        type == 'uuid')
    ) {
      return { typeIdentifier: 'ID', comment: null, error: null }
    }

    if (type === 'uuid') {
      return { typeIdentifier: 'String', comment: null, error: null }
    }
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
    if (type === 'timestamp with time zone') {
      return { typeIdentifier: 'DateTime', comment: null, error: null }
    }
    if (type === 'timestamp') {
      return { typeIdentifier: 'DateTime', comment: null, error: null }
    }
    if (type === 'json') {
      return { typeIdentifier: 'Json', comment: null, error: null }
    }
    if (type === 'date') {
      return { typeIdentifier: 'DateTime', comment: null, error: null }
    }

    return {
      typeIdentifier: null,
      comment: `Type '${type}' is not yet supported.`,
      error: `Not able to handle type '${type}'`,
    }
  }
}
