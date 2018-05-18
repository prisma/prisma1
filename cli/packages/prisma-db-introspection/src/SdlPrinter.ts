import { Table, Column, Relation } from './types/common'
import * as _ from 'lodash'

export interface RelationField {
  remoteColumn: Column
  remoteTable: Table
}

export class SdlPrinter {
  print(tables: Table[]): string {
    const candidates = tables.filter(x => !x.isJunctionTable)
    const sdl = _.map(candidates, table =>
      this.printType(table, tables.filter(x => x != table))
    )

    return sdl.join('\r\n')
  }

  printType(table: Table, otherTables: Table[]) {
    const nativeFields = table.columns
    const filterFunction = c => (c.relation || false) && c.relation.table == table.name
    const relationFields = otherTables
      .filter(t => t.columns.some(filterFunction))
      .map(t =>
        t.columns
          .filter(filterFunction)
          .map(c => ({ remoteColumn: c, remoteTable: t }))
      )
      .reduce((acc, next) => acc.concat(next), [])

    return `type ${this.capitalizeFirstLetter(table.name)} @pgTable(name: "${table.name}") {
  ${(_.map(nativeFields, nativeField => this.printField(nativeField)).concat(relationFields.map(field => this.printBackRelationField(field)))).join('\n  ')}
}
`
  }

  printBackRelationField(field: RelationField) {
    if (field.remoteTable.isJunctionTable) {
      const otherRemoteTableField = field.remoteTable.columns.filter(
        x => x.name !== field.remoteColumn.name
      )[0]
      const relatedTable = (otherRemoteTableField.relation as Relation).table

      return `${this.lowerCaseFirstLetter(
        relatedTable
      )}s: [${this.capitalizeFirstLetter(
        relatedTable
      )}!]! @pgRelationTable(table: "${field.remoteTable.name}" name: "${
        field.remoteTable.name
        }")`
    } else {
      return `${field.remoteTable.name}s: [${this.capitalizeFirstLetter(
        field.remoteTable.name
      )}!]!`
    }
  }

  printField(column: Column) {
    return `${this.printFieldName(column)}: ${this.printFieldType(
      column
    )}${this.printFieldOptional(column)}${this.printRelationDirective(
      column
    )}${this.printFieldDirectives(column)}`
  }

  printFieldName(column: Column) {
    if (column.relation) {
      return this.removeIdSuffix(column.name)
    } else if (column.isPrimaryKey) {
      return "id"
    } else {
      return column.name
    }
  }

  printFieldType(column: Column) {
    if (column.relation) {
      return this.capitalizeFirstLetter(column.relation.table)
    } else {
      return column.typeIdentifier
    }
  }

  printRelationDirective(column: Column) {
    if (column.relation) {
      return ` @pgRelation(column: "${column.name}")`
    } else {
      return ''
    }
  }

  printFieldOptional(column: Column) {
    return column.nullable ? '' : '!'
  }

  printFieldDirectives(column: Column) {
    let directives = ''
    if (column.isUnique) {
      directives += ` @unique`
    }

    if (column.isPrimaryKey && column.name != "id") {
      directives += ` @pgColumn(name: "${column.name}")`
    }

    if (column.defaultValue != null) {
      if (column.defaultValue == '[AUTO INCREMENT]') {
        return directives
      }

      if (
        column.typeIdentifier == 'String' ||
        column.typeIdentifier == 'DateTime' ||
        column.typeIdentifier == 'Json'
      ) {
        directives += ` @default(value: "${column.defaultValue}")`
      } else {
        directives += ` @default(value: ${column.defaultValue})`
      }
    }

    return directives
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
