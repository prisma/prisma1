
import { RelationalConnector } from '../relationalConnector'
import { Table, Column, TableRelation, PrimaryKey } from '../relationalConnector'
import * as _ from 'lodash'
import { Client } from 'pg';
import { TypeIdentifier, DatabaseType } from 'prisma-datamodel';
import { PostgresIntrospectionResult } from './postgresIntrospectionResult'

// Documentation: https://www.prisma.io/docs/data-model-and-migrations/introspection-mapping-to-existing-db-soi1/

// Responsible for extracting a normalized representation of a PostgreSQL database (schema)
export class PostgresConnector extends RelationalConnector {
  client: Client
  connectionPromise: Promise<any>

  constructor(client: Client) {
    super()
    this.client = client
    // TODO: This should not happen here.
    this.connectionPromise = this.client.connect()
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.postgres
  }

  public async listSchemas(): Promise<string[]> {
    await this.connectionPromise
    return await this.querySchemas()
  }

  public async introspect(schema: string): Promise<PostgresIntrospectionResult> {
    return new PostgresIntrospectionResult(await this.listModels(schema), this.getDatabaseType())
  }

  public async listModels(schemaName: string): Promise<Table[]> {
    await this.connectionPromise

    const [relations, tableColumns, primaryKeys] = await Promise.all([
      this.queryRelations(schemaName),
      this.queryTableColumns(schemaName),
      this.queryPrimaryKeys(schemaName),
    ])

    const tables = _.map(tableColumns, (rawColumns, tableName) => {
      const tablePrimaryKey =
        primaryKeys.find(pk => pk.tableName === tableName) || null
      const tableRelations = relations.filter(
        rel => rel.source_table === tableName,
      )
      const columns = _.map(rawColumns, column => {
        // Ignore composite keys for now
        const isPk = Boolean(
          tablePrimaryKey &&
            tablePrimaryKey.fields.length == 1 &&
            Boolean(tablePrimaryKey.fields.includes(column.column_name)),
        )

        const { typeIdentifier, comment, error } = this.toTypeIdentifier(
          column.data_type,
          column.column_name,
          isPk,
        )

        const col = {
          isUnique: column.is_unique || isPk,
          isPrimaryKey: isPk,
          defaultValue: this.parseDefaultValue(column.column_default),
          name: column.column_name,
          type: column.data_type,
          typeIdentifier,
          comment,
          nullable: column.is_nullable === 'YES',
        } as Column

        return col
      }).filter(x => x != null) as Column[]
      return new Table(tableName, columns, relations)
    })

    return _.sortBy(tables, x => x.name)
  }

  // Queries all columns of all tables in given schema and returns them grouped by table_name
  async queryTableColumns(
    schemaName: string,
  ): Promise<{ [key: string]: any[] }> {
    const res = await this.client.query(
      `SELECT *, (SELECT EXISTS(
         SELECT *
         FROM information_schema.table_constraints AS tc 
         JOIN information_schema.key_column_usage AS kcu
           ON tc.constraint_name = kcu.constraint_name
         WHERE constraint_type = 'UNIQUE' 
         AND tc.table_schema = $1::text
         AND tc.table_name = c.table_name
         AND kcu.column_name = c.column_name)) as is_unique
       FROM  information_schema.columns c
       WHERE table_schema = $1::text`,
      [schemaName.toLowerCase()],
    )

    return _.groupBy(res.rows, 'table_name')
  }

  async queryPrimaryKeys(schemaName: string): Promise<PrimaryKey[]> {
    return this.client
      .query(
        `SELECT tc.table_name, kc.column_name
       FROM information_schema.table_constraints tc
       JOIN information_schema.key_column_usage kc 
         ON kc.table_name = tc.table_name 
         AND kc.table_schema = tc.table_schema
         AND kc.constraint_name = tc.constraint_name
       WHERE tc.constraint_type = 'PRIMARY KEY'
       AND tc.table_schema = $1::text
       AND kc.ordinal_position IS NOT NULL
       ORDER BY tc.table_name, kc.position_in_unique_constraint;`,
        [schemaName.toLowerCase()],
      )
      .then(keys => {
        const grouped = _.groupBy(keys.rows, 'table_name')
        return _.map(grouped, (pks, key) => {
          return {
            tableName: key,
            fields: pks.map(x => x.column_name),
          } as PrimaryKey
        })
      })
  }

  async queryRelations(schemaName: string): Promise<TableRelation[]> {
    const res = await this.client.query(
      `SELECT source_table_name,
              source_attr.attname AS source_column,
              target_table_name,
              target_attr.attname AS target_column
      FROM pg_attribute target_attr, pg_attribute source_attr,
      (
        SELECT source_table_name, target_table_name, source_table_oid, target_table_oid, source_constraints[i] source_constraints, target_constraints[i] AS target_constraints
        FROM
        (
          SELECT pgc.relname as source_table_name, pgct.relname as target_table_name, conrelid as source_table_oid, confrelid AS target_table_oid, conkey AS source_constraints, confkey AS target_constraints, generate_series(1, array_upper(conkey, 1)) AS i
          FROM pg_constraint as pgcon 
            LEFT JOIN pg_class as pgc ON pgcon.conrelid = pgc.oid -- source table
            LEFT JOIN pg_namespace as pgn ON pgc.relnamespace = pgn.oid
            LEFT JOIN pg_class as pgct ON pgcon.confrelid = pgct.oid -- target table
            LEFT JOIN pg_namespace as pgnt ON pgct.relnamespace = pgnt.oid
          WHERE contype = 'f'
          AND pgn.nspname = $1::text 
          AND pgnt.nspname = $1::text 
        ) query1
      ) query2
      WHERE target_attr.attnum = target_constraints AND target_attr.attrelid = target_table_oid
      AND   source_attr.attnum = source_constraints AND source_attr.attrelid = source_table_oid;`,
      [schemaName.toLowerCase()],
    )

    return res.rows.map(row => {
      return {
        source_table: row.source_table_name,
        source_column: row.source_column,
        target_table: row.target_table_name,
        target_column: row.target_column,
      }
    }) as TableRelation[]
  }

  async querySchemas() {
    const res = await this.client.query(
      `SELECT schema_name 
       FROM information_schema.schemata 
       WHERE schema_name NOT LIKE 'pg_%' 
       AND schema_name NOT LIKE 'information_schema';`,
    )

    return res.rows.map(x => x.schema_name)
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

      if (withoutPrefix === 'NULL') {
        return null
      }

      return withoutPrefix
    }

    return string
  }

  toTypeIdentifier(
    type: string,
    field: string,
    isPrimaryKey: boolean,
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
        type === 'uuid')
    ) {
      return {
        typeIdentifier: type === 'uuid' ? 'UUID' : 'ID',
        comment: null,
        error: null,
      }
    }

    if (type === 'uuid') {
      return { typeIdentifier: 'UUID', comment: null, error: null }
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
