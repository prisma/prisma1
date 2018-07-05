import { Table, Column, TableRelation } from './types/common'
import { SDL, GQLType, GQLField } from './types/graphql'
import * as _ from 'lodash'
const pluralize = require('pluralize')

export class SDLInferrer {
  infer(dbTables: Table[]): SDL {
    const typeCandidates = dbTables.filter(t => !t.isJoinTable())
    const joinTables = dbTables.filter(t => t.isJoinTable())

    // Assemble basic types
    const types = typeCandidates.map(tc => {
      // tc.columns.some(f => f.isPrimaryKey)

      const name = this.capitalizeFirstLetter(tc.name)
      const directives = [`@pgTable(name: "${tc.name}")`]

      const fields: GQLField[] = tc.columns.map(column => {
        const directives = [
          ...(column.isUnique ? ["@unique"] : []),
          ...(column.isPrimaryKey && column.name !== 'id' ? [`@pgColumn(name: "${column.name}")`] : []),
          ...(column.defaultValue && column.defaultValue.trim() !== '[AUTO INCREMENT]' 
            ? [...(this.isStringableValue(column) 
              ? [`@default(value: "${column.defaultValue}")`]
              : [`@default(value: ${column.defaultValue})`])]
            : [])
        ]
        return new GQLField(
          column.isPrimaryKey ? 'id' : column.name,
          column.typeIdentifier,
          !column.nullable,
          directives,
          column.isPrimaryKey,
          column.comment ? column.comment : "",
          column.typeIdentifier ? false : true,
          null,
        )
      })

      const inlineRelations = tc.relations.filter(relation => {
        return relation.source_table === tc.name
      })
      const inlineRelationFields = inlineRelations.map(relation => {
        const directives = [
          `@pgRelation(column: "${relation.source_column}")`
        ]
        // TODO: Is this hacky, can a column have multiple relations 
        // - YES - what are the repurcussions of this assumption?
        const remoteColumn = _.intersectionWith(tc.columns, tc.relations, (a, b) => a.name === b.source_column)
        return new GQLField(
          this.removeIdSuffix(relation.source_column),
          `${this.capitalizeFirstLetter(relation.target_table)}`,
          remoteColumn && remoteColumn.length > 0 ? !remoteColumn[0].nullable : false,
          directives,
          false,
          "", // TODO: Figure out comment thing for this
          false,
          null,
        )
      })

      const relations = tc.relations.filter(relation => {
        return relation.target_table === tc.name
      })
      // console.log('ZEBRA RELATIONS: ', relations);
      const relationFields = relations.map(relation => {
        const duplicateRelations = relations.filter(r => relation.source_table === r.source_table )
        const fieldName = duplicateRelations.length > 1 
                ? pluralize(relation.source_table) + '_' + pluralize(
                  this.removeIdSuffix(relation.source_column)
                )
                : pluralize(relation.source_table)
        const directives = [
          ...(duplicateRelations.length > 1 ? [`@relation(name: "${fieldName}")`] : []) 
        ]
        return new GQLField(
          fieldName,
          `[${this.capitalizeFirstLetter(relation.source_table)}!]`,
          true,
          directives,
          false,
          "", // TODO: Figure out comment thing for this
          false,
          null,
        )
      })

      const relationTables = joinTables.reduce((relations, joinTable) => {
        if (joinTable.relations.some(relation => relation.target_table === tc.name)) {
          return relations.concat(joinTable.relations.filter(relation => relation.target_table !== tc.name))
        } else {
          return relations
        }
      }, [] as TableRelation[])

      const relationTableFields = relationTables.map(relation => {
        const directives = [
          `@pgRelationTable(table: "${relation.source_table}" name: "${relation.source_table}")`
        ]
        return new GQLField(
          pluralize(relation.target_table),
          `[${this.capitalizeFirstLetter(relation.target_table)}!]`,
          true,
          directives,
          false,
          "", // TODO: Figure out comment thing for this
          false,
          null,
        )
      })
      
      const allFields = [
        ...(_.differenceWith(fields, inlineRelationFields, (a, b) => {
          return this.removeIdSuffix(a.name) === this.removeIdSuffix(b.name)
        })),
        ...inlineRelationFields,
        ...(_.differenceWith(relationFields, relationTableFields, (a, b) => {
          // TODO: Manage this ugly hack if finding relation field in 
          // directive of relation table field 
          // there is also plural to singular hack in this
          return b.directives.join('').indexOf(pluralize.singular(a.name)) > -1
        })),
        ...relationTableFields
      ]

      const someValidFields = fields.some(field => field.isValid())
      return new GQLType(name, allFields, directives, !someValidFields)
    })

    return new SDL(types)
  }

  printType(table: Table, otherTables: Table[]) {
    const nativeFields = table.columns
    // const filterFunction = c => c.relation !== null && c.relation.table == table.name
    // const relationFields = otherTables
    //   .filter(t => t.columns.some(filterFunction))
    //   .map(t =>
    //     t.columns
    //       .filter(filterFunction)
    //       .map(c => ({ remoteColumn: c, remoteTable: t }))
    //   )
    //   .reduce((acc, next) => acc.concat(next), [])

    const raw = `type ${this.capitalizeFirstLetter(table.name)} @pgTable(name: "${table.name}") {
  ${(_.map(nativeFields, nativeField => this.printField(nativeField))/*.concat(relationFields.map(field => this.printBackRelationField(field)))*/).join('\n  ')}
}
`

    // if (table.hasPrimaryKey) {
    //   return raw
    // } else {
    const commented = raw.split(/\r?\n/).map(line => { return line.length > 0 ? `// ${line}` : "" }).join("\n")
    return `// Types without primary key not yet supported\n${commented}`
    // }
  }

  // printBackRelationField(field: RelationField) {
  //   if (field.remoteTable.isJunctionTable) {
  //     const otherRemoteTableField = field.remoteTable.columns.filter(
  //       x => x.name !== field.remoteColumn.name
  //     )[0]
  //     const relatedTable = (otherRemoteTableField.relation as Relation).table

  //     return `${this.lowerCaseFirstLetter(
  //       relatedTable
  //     )}s: [${this.capitalizeFirstLetter(
  //       relatedTable
  //     )}!]! @pgRelationTable(table: "${field.remoteTable.name}" name: "${
  //       field.remoteTable.name
  //       }")`
  //   } else {
  //     return `${field.remoteTable.name}s: [${this.capitalizeFirstLetter(
  //       field.remoteTable.name
  //     )}!]!`
  //   }
  // }

  printField(column: Column) {
    const field = `${this.printFieldName(column)}: ${this.printFieldType(
      column
    )}${this.printFieldOptional(column)}${this.printRelationDirective(
      column
    )}${this.printFieldDirectives(column)}${
      column.comment === null ? "" : ` # ${column.comment}`}`

    if (column.typeIdentifier === null) {
      return `# ${field}`
    }

    return field
  }

  printFieldName(column: Column) {
    // if (column.relation !== null) {
    //   return this.removeIdSuffix(column.name)
    // } else if (column.isPrimaryKey) {
    // return "id"
    // } else {
    return column.name
    // }
  }

  printFieldType(column: Column) {
    // if (column.relation !== null) {
    // return this.capitalizeFirstLetter(column.relation.target_table)
    // } else {
    return column.typeIdentifier
    // }
  }

  printRelationDirective(column: Column) {
    // if (column.relation !== null) {
    // return ` @pgRelation(column: "${column.name}")`
    // } else {
    return ''
    // }
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

  isStringableValue(column) {
    if (
      column.typeIdentifier == 'String' ||
      column.typeIdentifier == 'DateTime' ||
      column.typeIdentifier == 'Json'
    ) {
      return true
    } else {
      return false
    }
  }
}
