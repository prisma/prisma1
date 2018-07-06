import { Table, Column, TableRelation } from './types/common'
import { SDL, GQLType, GQLField } from './types/graphql'
import * as _ from 'lodash'
const pluralize = require('pluralize')
const upperCamelCase = require('uppercamelcase');

export class SDLInferrer {
  infer(dbTables: Table[]): SDL {
    const typeCandidates = dbTables.filter(t => !t.isJoinTable())
    const joinTables = dbTables.filter(t => t.isJoinTable())

    // Assemble basic types
    const types = typeCandidates.map(tc => {
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
          column.typeIdentifier ? false : true
        )
      })

      const inlineRelations = tc.relations.filter(relation => {
        return relation.source_table === tc.name
      })
      const inlineRelationFields = inlineRelations.map(relation => {
        // TODO: Can unify remoteColumns and ambiguousRelations
        // into one structure
        const remoteColumns = _.intersectionWith(
          tc.columns,
          tc.relations.filter(innerRelation => innerRelation.source_table === relation.source_table && innerRelation.target_table === relation.target_table),
          (a, b) => a.name === b.source_column
        ).filter(remoteColumn => remoteColumn.name === relation.source_column);        
        const ambiguousRelations = tc.relations.filter(innerRelation => innerRelation.source_table === relation.source_table && innerRelation.target_table === relation.target_table)

        const relationName = pluralize(relation.source_table) + '_' + pluralize(this.removeIdSuffix(relation.source_column))
        const directives = [
          `@pgRelation(column: "${relation.source_column}")`,
          ...(ambiguousRelations.length > 1 && remoteColumns && remoteColumns.length > 0 ? [`@relation(name: "${upperCamelCase(relationName)}")`] : [])
        ]

        return new GQLField(
          this.removeIdSuffix(relation.source_column),
          `${this.capitalizeFirstLetter(relation.target_table)}`,
          false,
          directives,
          false,
          "", // TODO: Figure out comment thing for this
          false
        )
      })

      const relations = tc.relations.filter(relation => {
        return relation.target_table === tc.name
      })
      const relationFields = relations.map(relation => {
        const duplicateRelations = relations.filter(r => relation.source_table === r.source_table )
        const fieldName = duplicateRelations.length > 1 
                ? pluralize(relation.source_table) + '_' + pluralize(
                  this.removeIdSuffix(relation.source_column)
                )
                : pluralize(relation.source_table)
        const directives = [
          ...(duplicateRelations.length > 1 ? [`@relation(name: "${upperCamelCase(fieldName)}")`] : []) 
        ]
        return new GQLField(
          fieldName,
          `[${this.capitalizeFirstLetter(relation.source_table)}!]`,
          true,
          directives,
          false,
          "", // TODO: Figure out comment thing for this
          false
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
          false
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
