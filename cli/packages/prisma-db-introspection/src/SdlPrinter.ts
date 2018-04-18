import { Table, Column } from './types/common'
import * as _ from 'lodash'

export interface RelationField {
  remoteColumn: Column
  remoteTable: string
}

export class SdlPrinter {
  async print(tables: Table[]): Promise<string> {
    const sdl = _.map(tables, table =>
      this.printType(table, tables.filter(x => x != table)),
    )

    return sdl.join('\r\n')
  }

  printType(table: Table, otherTables: Table[]) {
    const nativeFields = table.columns
    const filterFunction = c =>
      (c.relation || false) && c.relation.table == table.name
    const relationFields = otherTables
      .filter(t => t.columns.some(filterFunction))
      .map(t =>
        t.columns
          .filter(filterFunction)
          .map(c => ({ remoteColumn: c, remoteTable: t.name })),
      )
      .reduce((acc, next) => acc.concat(next), [])

    return `type ${this.capitalizeFirstLetter(table.name)} @postgres(table: "${
      table.name
    }") {${_.map(nativeFields, column => this.printField(column)).join(
      '',
    )}${relationFields.map(field => this.printRelationField(field)).join('')}
}`
  }

  printRelationField(field: RelationField) {
    return `\r\n  ${field.remoteTable}s: [${this.capitalizeFirstLetter(
      field.remoteTable,
    )}] @postgres(foreignColumn: "${field.remoteColumn.name}")`
  }

  printField(column: Column) {
    return `\r\n  ${this.printFieldName(column)}: ${this.printFieldType(
      column,
    )}${this.printFieldOptional(column)}${this.printFieldDirective(column)}`
  }

  printFieldName(column: Column) {
    return column.name
  }

  printFieldType(column: Column) {
    if (column.relation) {
      return (
        this.capitalizeFirstLetter(column.relation.table) +
        ` @postgres(ownColumn: "${column.name}")`
      )
    } else {
      return column.typeIdentifier
    }
  }

  printFieldOptional(column: Column) {
    return column.nullable ? '' : '!'
  }

  printFieldDirective(column: Column) {
    if (column.relation) {
      return ``
    } else return ''
  }

  capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1)
  }
}
