import { IConnector } from '../../common/connector'
import { TypeIdentifier, DatabaseType } from 'prisma-datamodel'
import { RelationalIntrospectionResult } from './relationalIntrospectionResult'
import IDatabaseClient from '../IDatabaseClient'

export interface IInternalIndexInfo {
  tableName: string
  name: string
  fields: string[]
  unique: boolean
  isPrimaryKey: boolean
}

export interface IInternalEnumInfo {
  name: string
  values: string[]
}

export abstract class RelationalConnector implements IConnector {
  protected client: IDatabaseClient

  constructor(client: IDatabaseClient) {
    this.client = client
  }

  abstract getDatabaseType(): DatabaseType
  protected abstract createIntrospectionResult(
    models: ITable[],
    relations: ITableRelation[],
    enums: IEnum[],
    sequences: ISequenceInfo[],
  ): RelationalIntrospectionResult

  protected async query(query: string, params: any[] = []): Promise<any[]> {
    return await this.client.query(query, params)
  }

  /**
   * Column comments are DB specific
   */
  protected abstract async queryColumnComment(
    schemaName: string,
    tableName: string,
    columnName: string,
  ): Promise<string | null>

  /**
   * Indices are DB specific
   */
  protected abstract async queryIndices(
    schemaName: string,
    tableName: string,
  ): Promise<IInternalIndexInfo[]>

  protected abstract async queryEnums(schemaName: string): Promise<IEnum[]>

  protected abstract async listSequences(
    schemaName: string,
  ): Promise<ISequenceInfo[]>

  public async introspect(
    schema: string,
  ): Promise<RelationalIntrospectionResult> {
    return this.createIntrospectionResult(
      await this.listModels(schema),
      await this.listRelations(schema),
      await this.listEnums(schema),
      await this.listSequences(schema),
    )
  }

  public async listEnums(schemaName: string): Promise<IEnum[]> {
    return await this.queryEnums(schemaName)
  }

  /**
   * All queries below use the standardized information_schema table.
   */
  public async listSchemas(): Promise<string[]> {
    const res = await this.query(
      `SELECT 
         schema_name
       FROM 
         information_schema.schemata
       WHERE schema_name NOT LIKE 'information_schema';`,
    )

    return res.map(x => x.schema_name)
  }

  protected async listModels(schemaName: string): Promise<ITable[]> {
    const tables: ITable[] = []
    const allTables = await this.queryTables(schemaName)

    for (const tableName of allTables) {
      const columns = await this.queryColumns(schemaName, tableName)

      for (const column of columns) {
        column.comment = await this.queryColumnComment(
          schemaName,
          tableName,
          column.name,
        )
      }

      const allIndices = await this.queryIndices(schemaName, tableName)
      const secondaryIndices = allIndices.filter(x => !x.isPrimaryKey)
      const [primaryKey] = allIndices.filter(x => x.isPrimaryKey)

      tables.push({
        name: tableName,
        columns: columns,
        indices: secondaryIndices,
        primaryKey: primaryKey || null,
      })
    }

    return tables
  }

  protected async queryTables(schemaName: string) {
    const allTablesQuery = `
      SELECT 
        table_name
      FROM 
        information_schema.tables
      WHERE 
        table_schema = ${this.parameter(1, 'text')}
        -- Views are not supported yet
        AND table_type = 'BASE TABLE'`

    return (await this.query(allTablesQuery, [schemaName])).map(
      row => row.table_name as string,
    )
  }

  /**
   * The name of the type column in information_schema.columns.
   *
   * The standardized DATA_TYPE field sitself is too unspecific.
   */
  protected abstract getTypeColumnName()

  /**
   * A condition on information_schema.columns, which shall
   * return true if the column has auto_increment set,
   * false otherwise.
   *
   * If the database supports sequences, this should simply return false.
   */
  protected abstract getAutoIncrementCondition()

  /**
   * Generates a parameter expression for the given SQL dialect.
   */
  protected abstract parameter(count: number, type: string)

  protected async queryColumns(schemaName: string, tableName: string) {
    const allColumnsQuery = `
      SELECT
        ordinal_position,
        column_name,
        ${this.getTypeColumnName()} as udt_name,
        column_default,
        is_nullable = 'YES' as is_nullable,
        ${this.getAutoIncrementCondition()} as is_auto_increment
      FROM
        information_schema.columns
      WHERE
        table_schema = '${schemaName}'
        AND table_name  = '${tableName}'`

    /**
     * Note, that ordinal_position comes back as a string because it's a bigint!
     */

    return (await this.query(allColumnsQuery)).map(row => {
      return {
        name: row.column_name as string,
        type: row.udt_name as string,
        isList: false,
        readOnly: false, // Thread nothing as read only for now.
        isUnique: false, // Will resolve via unique indexes later.
        isAutoIncrement: row.is_auto_increment,
        defaultValue: row.column_default as string,
        isNullable: row.is_nullable as boolean,
        comment: null as string | null,
      }
    })
  }

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
      INNER JOIN
        information_schema.key_column_usage AS keyColumn2
        ON keyColumn2.constraint_catalog = refConstraints.unique_constraint_catalog
        AND keyColumn2.constraint_schema = refConstraints.unique_constraint_schema
        AND keyColumn2.constraint_name = refConstraints.unique_constraint_name
        AND keyColumn2.ordinal_position = keyColumn1.ordinal_position
      WHERE
        refConstraints.constraint_schema = ${this.parameter(1, 'text')}`

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
}

export interface IEnum {
  name: string
  values: string[]
}

export interface ITable {
  name: string
  columns: IColumn[]
  indices: IIndex[]
  primaryKey: IIndex | null
}

export interface IColumn {
  name: string
  isUnique: boolean
  defaultValue: any
  type: string
  comment: string | null
  isNullable: boolean
  isList: boolean
  /**
   * Indicates an auto_increment column. Only use this if the
   * database DOES NOT support sequences.
   */
  isAutoIncrement: boolean
}

export interface ISequenceInfo {
  name: string
  initialValue: number
  allocationSize: number
}

export interface ITableRelation {
  sourceTable: string
  targetTable: string
  sourceColumn: string
  targetColumn: string
}

export interface IIndex {
  name: string
  fields: string[]
  unique: boolean
}
