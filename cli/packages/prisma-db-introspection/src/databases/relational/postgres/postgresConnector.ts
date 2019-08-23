import {
  RelationalConnector,
  ITable,
  ITableRelation,
  IInternalEnumInfo,
  IEnum,
  ISequenceInfo,
} from '../relationalConnector'
import { Client } from 'pg'
import { DatabaseType } from 'prisma-datamodel'
import { PostgresIntrospectionResult } from './postgresIntrospectionResult'
import { RelationalIntrospectionResult } from '../relationalIntrospectionResult'
import IDatabaseClient from '../../IDatabaseClient'
import PostgresDatabaseClient from './postgresDatabaseClient'
import { DatabaseMetadata } from '../../../common/introspectionResult'
import * as debug from 'debug'

let log = debug('PostgresIntrospection')

// Documentation: https://www.prisma.io/docs/data-model-and-migrations/introspection-mapping-to-existing-db-soi1/

// Responsible for extracting a normalized representation of a PostgreSQL database (schema)
export class PostgresConnector extends RelationalConnector {
  constructor(client: IDatabaseClient | Client) {
    if (client instanceof Client) {
      client = new PostgresDatabaseClient(client)
    }
    super(client)
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.postgres
  }

  protected createIntrospectionResult(
    models: ITable[],
    relations: ITableRelation[],
    enums: IEnum[],
    sequences: ISequenceInfo[],
  ): RelationalIntrospectionResult {
    log('Creating postgres specific introspection result.')
    return new PostgresIntrospectionResult(models, relations, enums, sequences)
  }

  public async listSchemas(): Promise<string[]> {
    log('Listing schemas.')
    const schemas = await super.listSchemas()
    return schemas.filter(schema => !schema.startsWith('pg_'))
  }

  protected getTypeColumnName() {
    return 'udt_name'
  }

  protected getAutoIncrementCondition() {
    return 'false'
  }

  protected parameter(count: number, type: string) {
    return `$${count}::${type}`
  }

  // TODO: Unit test for column comments
  protected async queryColumnComments(schemaName: string) {
    log(`Querying column comments for ${schemaName}`)
    const commentQuery = `
      SELECT
      (
        SELECT
          pg_catalog.col_description(c.oid, cols.ordinal_position::int)
        FROM pg_catalog.pg_class c
        WHERE
          c.oid     = (SELECT ('"' || cols.table_schema || '"."' || cols.table_name || '"')::regclass::oid) AND
          c.relname = cols.table_name
      ) as column_comment, cols.column_name as column_name, cols.table_name as table_name
      FROM
        information_schema.columns cols
      WHERE
        cols.table_schema  = $1::text
    `
    const comments = (await this.query(commentQuery, [schemaName]))
      .filter(row => row.column_comment != undefined)
      .map(row => ({
        text: row.column_comment as string,
        columnName: row.column_name as string,
        tableName: row.table_name as string,
      }))

    return comments
  }

  protected async queryIndices(schemaName: string) {
    log(`Querying indices for table ${schemaName}.`)
    const indexQuery = `
      SELECT
          tableInfos.relname as table_name,
          indexInfos.relname as index_name,
          array_to_string(array_agg(columnInfos.attname), ',') as column_names,
          rawIndex.indisunique as is_unique,
          rawIndex.indisprimary as is_primary_key
      FROM
          -- pg_class stores infos about tables, indices etc: https://www.postgresql.org/docs/9.3/catalog-pg-class.html
          pg_class tableInfos,
          pg_class indexInfos,
          -- pg_index stores indices: https://www.postgresql.org/docs/9.3/catalog-pg-index.html
          pg_index rawIndex,
          -- pg_attribute stores infos about columns: https://www.postgresql.org/docs/9.3/catalog-pg-attribute.html
          pg_attribute columnInfos,
          -- pg_namespace stores info about the schema
          pg_namespace schemaInfo
      WHERE
          -- find table info for index
          tableInfos.oid = rawIndex.indrelid
          -- find index info
          AND indexInfos.oid = rawIndex.indexrelid
          -- find table columns
          AND columnInfos.attrelid = tableInfos.oid
          AND columnInfos.attnum = ANY(rawIndex.indkey)
          -- we only consider oridnary tables
          AND tableInfos.relkind = 'r'
          -- we only consider stuff out of one specific schema
          AND tableInfos.relnamespace = schemaInfo.oid
          AND schemaInfo.nspname = $1::text
      GROUP BY
          tableInfos.relname,
          indexInfos.relname,
          rawIndex.indisunique,
          rawIndex.indisprimary
    `
    return (await this.query(indexQuery, [schemaName])).map(row => {
      return {
        tableName: row.table_name as string,
        name: row.index_name as string,
        fields: this.parseJoinedArray(row.column_names),
        unique: row.is_unique as boolean,
        isPrimaryKey: row.is_primary_key as boolean,
      }
    })
  }

  private parseJoinedArray(arrayAsString: string): string[] {
    if (arrayAsString === null || arrayAsString === undefined) {
      return []
    }
    return arrayAsString.split(',').map(x => x.trim())
  }

  protected async queryEnums(schemaName: string): Promise<IInternalEnumInfo[]> {
    log(`Querying enums for schema ${schemaName}.`)
    const enumQuery = `
      SELECT
        t.typname AS "enumName",  
        array_to_string(array_agg(e.enumlabel), ',') AS "enumValues"
      FROM pg_type t 
        JOIN pg_enum e ON t.oid = e.enumtypid  
        JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
      WHERE 
          n.nspname = $1::text
      GROUP BY t.typname`

    return (await this.query(enumQuery, [schemaName])).map(row => {
      return {
        name: row.enumName as string,
        values: this.parseJoinedArray(row.enumValues),
      }
    })
  }

  protected async listSequences(schemaName: string): Promise<ISequenceInfo[]> {
    log('Querying sqeuences.')
    const sequenceQuery = `
    SELECT
      sequence_name, start_value
      FROM 
      information_schema.sequences
      WHERE
      sequence_schema =  $1::text`

    return (await this.query(sequenceQuery, [schemaName])).map(row => {
      return {
        name: row.sequence_name as string,
        initialValue: row.start_value as number,
        allocationSize: 1,
      }
    })
  }

  public async getMetadata(schemaName: string): Promise<DatabaseMetadata> {
    const schemaSizeQuery = `
      SELECT 
        SUM(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename)))::BIGINT as size
        FROM pg_tables WHERE schemaname = $1::text`

    const [{ size }] = await this.query(schemaSizeQuery, [schemaName])
    const count = await super.countTables(schemaName)

    return {
      countOfTables: count,
      sizeInBytes: size,
    }
  }
}
