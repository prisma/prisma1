import {
  RelationalConnector,
  ITable,
  IColumn,
  ITableRelation,
  IIndex,
  IInternalEnumInfo,
  IEnum,
} from '../relationalConnector'
import * as _ from 'lodash'
import { Connection } from 'mysql'
import { TypeIdentifier, DatabaseType, camelCase } from 'prisma-datamodel'
import { MysqlIntrospectionResult } from './mysqlIntrospectionResult'
import { RelationalIntrospectionResult } from '../relationalIntrospectionResult'
import IDatabaseClient from '../../IDatabaseClient'
import MysqlDatabaseClient from './mysqlDatabaseClient'

// Documentation: https://www.prisma.io/docs/data-model-and-migrations/introspection-mapping-to-existing-db-soi1/

// Responsible for extracting a normalized representation of a PostgreSQL database (schema)
export class MysqlConnector extends RelationalConnector {
  constructor(client: IDatabaseClient | Connection) {
    if((client as Connection).state !== undefined) {
      client = new MysqlDatabaseClient(client as Connection)
    }

    super(client as IDatabaseClient)
  }

  public getDatabaseType(): DatabaseType {
    return DatabaseType.postgres
  }

  protected createIntrospectionResult(
    models: ITable[],
    relations: ITableRelation[],
    enums: IEnum[]
  ): RelationalIntrospectionResult {
    return new MysqlIntrospectionResult(models, relations, enums)
  }

  protected getIsNullableConstant() {
    return "1"
  }


  protected getTypeColumnName() {
    return 'COLUMN_TYPE'
  }


  // TODO: Unit test for column comments
  protected async queryColumnComment(
    schemaName: string,
    tableName: string,
    columnName: string,
  ) {
    const commentQuery = `
      SELECT
        column_comment
      FROM 
        information_schema.columns
      WHERE 
        table_schema = $1::text
        AND table_name = $2::text
        AND column_name = $3::text
    `
    const [comment] = (await this.query(commentQuery, [
      schemaName,
      tableName,
      columnName,
    ])).map(row => row.column_comment as string)

    if (comment === undefined) {
      return null
    } else {
      return comment
    }
  }

  protected async queryIndices(schemaName: string, tableName: string) {
    const indexQuery = `
      SELECT 
        table_name, 
        index_name, 
        GROUP_CONCAT(DISTINCT column_name SEPARATOR ', ') AS column_names, 
        NOT non_unique AS is_unique, 
        index_name = 'PRIMARY' AS is_primary_key 
      FROM 
        information_schema.statistics
      WHERE
        table_schema = $1::text
        AND table_name = $2::text
      GROUP BY
        table_name, index_name, non_unique
    `
    return (await this.query(indexQuery, [schemaName, tableName])).map(row => {
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
    if(arrayAsString === null || arrayAsString === undefined) {
      return []
    }
    return arrayAsString.split(',').map(x => x.trim())
  }


  protected async queryEnums(schemaName: string): Promise<IInternalEnumInfo[]> {
    const enumQuery = `
      SELECT DISTINCT 
        column_type, table_name, column_name
      FROM
        information_schema.columns 
      WHERE 
        column_type like 'enum(%'`

      return (await this.query(enumQuery, [schemaName])).map(row => {
        const enumValues = row.enumValues as string
        // Strip 'enum(' from beginning and ')' from end.
        const strippedEnumValues = enumValues.substring(5, enumValues.length - 1)

        return {
          // Enum types in mysql are anonymous. We generate some funny name for them.
          name: camelCase(row.table_name) + camelCase(row.column_name) + 'Enum' as string,
          values: this.parseJoinedArray(strippedEnumValues)
        }
      })
  }
}
