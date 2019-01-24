import { RelationalConnector, ITable, IColumn, ITableRelation, IIndex } from '../relationalConnector'
import * as _ from 'lodash'
import { Client } from 'pg';
import { TypeIdentifier, DatabaseType } from 'prisma-datamodel';
import { PostgresIntrospectionResult } from './postgresIntrospectionResult'
import { RelationalIntrospectionResult } from '../relationalIntrospectionResult';

// Documentation: https://www.prisma.io/docs/data-model-and-migrations/introspection-mapping-to-existing-db-soi1/

// Responsible for extracting a normalized representation of a PostgreSQL database (schema)
export class PostgresConnector extends RelationalConnector {
  client: Client

  constructor(client: Client) {
    super()

    if (!(client instanceof Client)) {
      throw new Error('Postgres instance needed for initialization.')
    }

    this.client = client
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.postgres
  }

  protected createIntrospectionResult(models: ITable[], relations: ITableRelation[]): RelationalIntrospectionResult {
    return new PostgresIntrospectionResult(models, relations)
  }

  protected async query(query: string, params?: any[]): Promise<any[]> {
    return (await this.client.query(query, params)).rows
  }


  public async listSchemas(): Promise<string[]> {
    const schemas = await super.listSchemas()
    return schemas.filter(schema => !schema.startsWith('pg_'))
  }

  // TODO: Unit test for column comments
  protected async queryColumnComment(schemaName: string, tableName: string, columnName: string) {
    const commentQuery = `
      SELECT
      (
        SELECT
          pg_catalog.col_description(c.oid, cols.ordinal_position::int)
        FROM pg_catalog.pg_class c
        WHERE
          c.oid     = (SELECT ('"' || cols.table_schema || '"."' || cols.table_name || '"')::regclass::oid) AND
          c.relname = cols.table_name
      ) as column_comment   
      FROM
        information_schema.columns cols
      WHERE
        cols.table_schema  = $1::text AND
        cols.table_name    = $2::text AND
        cols.column_name   = $3::text;
    `
    const [comment] = (await this.query(commentQuery, [schemaName, tableName, columnName])).map(row => row.column_comment as string)

    if(comment === undefined) {
      return null
    } else {
      return comment
    }
  }

  protected async queryIndices(schemaName: string, tableName: string) {
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
          AND tableInfos.relname = $2::text
      GROUP BY
          tableInfos.relname,
          indexInfos.relname,
          rawIndex.indisunique,
          rawIndex.indisprimary
    `
    return (await this.query(indexQuery, [schemaName, tableName])).map(row => { return {
      tableName: row.table_name as string,
      name: row.index_name as string,
      fields: this.parseIndexColumns(row.column_names),
      unique: row.is_unique as boolean,
      isPrimaryKey: row.is_primary_key as boolean
    }})
  }

  private parseIndexColumns(arrayAsString: string): string[] {
    return arrayAsString.split(',').map(x => x.trim())
  }
}