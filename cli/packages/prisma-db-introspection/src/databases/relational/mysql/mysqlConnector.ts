import {
  RelationalConnector,
  ITable,
  ITableRelation,
  IInternalEnumInfo,
  IEnum,
  ISequenceInfo,
} from '../relationalConnector'
import { Connection } from 'mysql'
import { DatabaseType, capitalize } from 'prisma-datamodel'
import { MysqlIntrospectionResult } from './mysqlIntrospectionResult'
import { RelationalIntrospectionResult } from '../relationalIntrospectionResult'
import { DatabaseMetadata } from '../../../common/introspectionResult'
import IDatabaseClient from '../../IDatabaseClient'
import MysqlDatabaseClient from './mysqlDatabaseClient'

// Documentation: https://www.prisma.io/docs/data-model-and-migrations/introspection-mapping-to-existing-db-soi1/

// Responsible for extracting a normalized representation of a PostgreSQL database (schema)
export class MysqlConnector extends RelationalConnector {
  constructor(client: IDatabaseClient | Connection) {
    if ((client as Connection).state !== undefined) {
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
    enums: IEnum[],
    sequences: ISequenceInfo[],
  ): RelationalIntrospectionResult {
    return new MysqlIntrospectionResult(models, relations, enums, sequences)
  }

  protected getTypeColumnName() {
    return 'COLUMN_TYPE'
  }
  protected getAutoIncrementCondition() {
    return 'EXTRA like \'%auto_increment%\''
  }

  protected parameter(count: number, type: string) {
    return `?`
  }

  protected hasReferentialConstraintsTableName() {
    return true
  }

  // TODO: Unit test for column comments
  protected async queryColumnComments(schemaName: string) {
    const commentQuery = `
      SELECT
        column_comment,
        column_name,
        table_name
      FROM
        information_schema.columns
      WHERE
        table_schema = ?
        AND column_comment != ''
    `
    const comments = (await this.query(commentQuery, [schemaName])).map(
      row => ({
        text: row.column_comment as string,
        columnName: row.column_name as string,
        tableName: row.table_name as string,
      }),
    )

    return comments
  }

  protected async queryIndices(schemaName: string) {
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
        table_schema = '${schemaName}'
      GROUP BY
        table_name, index_name, non_unique
    `
    return (await this.query(indexQuery)).map(row => {
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
    return arrayAsString.split(',').map(x => x.trim().replace(/'/g, ''))
  }

  protected async queryEnums(schemaName: string): Promise<IInternalEnumInfo[]> {
    const enumQuery = `
      SELECT DISTINCT
        column_type, table_name, column_name
      FROM
        information_schema.columns
      WHERE 
        column_type like 'enum(%'
        AND table_schema = ?`

    return (await this.query(enumQuery, [schemaName])).map(row => {
      const enumValues = row.column_type as string
      // Strip 'enum(' from beginning and ')' from end.
      const strippedEnumValues = enumValues.substring(5, enumValues.length - 1)

      return {
        // Enum types in mysql are anonymous. We generate some funny name for them.
        name: (capitalize(row.table_name) +
          capitalize(row.column_name) +
          'Enum') as string,
        values: this.parseJoinedArray(strippedEnumValues),
      }
    })
  }

  /**
   * We have extra join conditions in mysql.
   * @param schemaName
   */
  protected async listRelations(schemaName: string): Promise<ITableRelation[]> {
    const fkQuery = `  
      SELECT 
        keyColumn1.constraint_name AS "fkConstraintName",
        keyColumn1.table_name AS "fkTableName", 
        keyColumn1.column_name AS "fkColumnName", 
        keyColumn2.constraint_name AS "referencedConstraintName",
        keyColumn2.table_name AS "referencedTableName", 
        keyColumn2.column_name AS "referencedColumnName" 
      FROM 
        information_schema.referential_constraints refConstraints
      INNER JOIN
        information_schema.key_column_usage AS keyColumn1
        ON keyColumn1.constraint_catalog = refConstraints.constraint_catalog
        AND keyColumn1.constraint_schema = refConstraints.constraint_schema
        AND keyColumn1.constraint_name = refConstraints.constraint_name
        -- Extra join needed in mysql
        AND keyColumn1.table_name = refConstraints.table_name
      INNER JOIN
        information_schema.key_column_usage AS keyColumn2
        ON keyColumn2.constraint_catalog = refConstraints.unique_constraint_catalog
        AND keyColumn2.constraint_schema = refConstraints.unique_constraint_schema
        AND keyColumn2.constraint_name = refConstraints.unique_constraint_name
        AND keyColumn2.ordinal_position = keyColumn1.ordinal_position
        -- Extra join needed in mysql
        AND keyColumn2.table_name = refConstraints.referenced_table_name
      WHERE
        refConstraints.constraint_schema = ?`

    const result = (await this.query(fkQuery, [schemaName])).map(row => {
      return {
        sourceColumn: row.fkColumnName as string,
        sourceTable: row.fkTableName as string,
        targetColumn: row.referencedColumnName as string,
        targetTable: row.referencedTableName as string,
      }
    })

    return result
  }

  protected async listSequences(schemaName: string): Promise<ISequenceInfo[]> {
    return []
  }

  public async getMetadata(schemaName: string): Promise<DatabaseMetadata> {
    const schemaSizeQuery = `
    SELECT 
      SUM(data_length + index_length) as size 
      FROM information_schema.TABLES 
      WHERE table_schema = ?`

    const [{ size }] = await this.query(schemaSizeQuery, [schemaName])
    const count = await super.countTables(schemaName)

    return {
      countOfTables: count,
      sizeInBytes: size,
    }
  }
}
