import { Table, Column, Relation } from './types/common'
import * as _ from 'lodash'

export interface RelationField {
  remoteColumn: Column
  remoteTable: Table
}

export class SdlPrinter {
  async print(tables: Table[]): Promise<string> {
    const candidates = tables.filter(x => !x.isJunctionTable)

    const sdl = _.map(candidates, table =>
      this.printType(table, tables.filter(x => x != table))
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
          .map(c => ({ remoteColumn: c, remoteTable: t }))
      )
      .reduce((acc, next) => acc.concat(next), [])

    return `type ${this.capitalizeFirstLetter(table.name)} @pgTable(name: "${
      table.name
    }") {${_.map(nativeFields, column => this.printField(column)).join(
      ''
    )}${relationFields
      .map(field => this.printBackRelationField(field))
      .join('')}
}
`
  }

  printBackRelationField(field: RelationField) {
    if (field.remoteTable.isJunctionTable) {
      const otherRemoteTableField = field.remoteTable.columns.filter(
        x => x.name !== field.remoteColumn.name
      )[0]
      const relatedTable = (otherRemoteTableField.relation as Relation).table

      return `\n  ${this.lowerCaseFirstLetter(
        relatedTable
      )}s: [${this.capitalizeFirstLetter(
        relatedTable
      )}] @pgRelationTable(table: "${field.remoteTable.name}" name: "${
        field.remoteTable.name
      }")`
    } else {
      return `\n  ${field.remoteTable.name}s: [${this.capitalizeFirstLetter(
        field.remoteTable.name
      )}]`
    }
  }

  printField(column: Column) {
    return `\n  ${this.printFieldName(column)}: ${this.printFieldType(
      column
    )}${this.printFieldOptional(column)}${this.printFieldDirective(column)}`
  }

  printFieldName(column: Column) {
    if (column.relation) {
      return this.removeIdSuffix(column.name)
    } else {
      return column.name
    }
  }

  printFieldType(column: Column) {
    if (column.relation) {
      return (
        this.capitalizeFirstLetter(column.relation.table) +
        ` @pgRelation(column: "${column.name}")`
      )
    } else {
      return column.typeIdentifier
    }
  }

  printFieldOptional(column: Column) {
    return column.nullable ? '' : '!'
  }

  printFieldDirective(column: Column) {
    if (column.isUnique) {
      return ` @unique`
    }
    if (column.defaultValue != null) {
      if (
        column.typeIdentifier == 'String' ||
        column.typeIdentifier == 'DateTime' ||
        column.typeIdentifier == 'Json'
      ) {
        return ` @default(value = "${column.defaultValue}")`
      } else {
        return ` @default(value = ${column.defaultValue})`
      }
    }

    return ''
  }

  capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1)
  }

  lowerCaseFirstLetter(string) {
    return string.charAt(0).toLowerCase() + string.slice(1)
  }

  removeIdSuffix(string) {
    function removeSuffix(suffix, string) {
      if (string.endsWith(suffix)) {
        return string.substring(0, string.length - suffix.length)
      } else {
        return string
      }
    }

    return removeSuffix('_ID', removeSuffix('_id', removeSuffix('Id', string)))
  }
}
