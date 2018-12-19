import { Table, TableRelation } from "../relationalConnector"
import { RelationalIntrospectionResult } from "../relationalIntrospectionResult"
import { ISDL, IGQLField, IGQLType, IDirectiveInfo, plural, camelCase, capitalize } from 'prisma-datamodel'
import * as _ from 'lodash'

// TODO: This class holds too much logic.
// TODO: This class duplicates too much logic and has too many assumptions about how types may look. 
export class PostgresIntrospectionResult extends RelationalIntrospectionResult {
  async infer(dbTables: Table[]): Promise<ISDL> {
    const typeCandidates = dbTables.filter(t => !t.isJoinTable())
    const joinTables = dbTables.filter(t => t.isJoinTable())

    // Assemble basic types
    const types = typeCandidates.map(tc => {
      const name = capitalize(tc.name)
      const directives: IDirectiveInfo[] = [{
        name: 'pgTable',
        arguments: {
          name: `"${tc.name}"`
        }
      }]

      const fields: IGQLField[] = tc.columns.map(column => {
        
        const directives: IDirectiveInfo[] = []

        if(column.isPrimaryKey && column.name !== 'id') {
          directives.push({
            name: "pgColumn",
            arguments: {
              name: `"${column.name}"`
            }
          })
        }

        // TODO: Remove magic AUTO INCREMENT constant. 
        const defaultValue = column.defaultValue && column.defaultValue.trim() !== '[AUTO INCREMENT]' ? column.defaultValue : null
        const isUnique = column.isUnique

        return {
          name: column.isPrimaryKey ? 'id' : column.name,
          type: column.typeIdentifier || '<Unknown>',
          defaultValue, 
          isId: column.isPrimaryKey, 
          isList: false,
          isReadOnly: false,
          // TODO: We should turn of isRequired in case of auto-increment. 
          isRequired: !column.nullable,
          isUnique: column.isUnique,
          relatedField: null,
          relationName: null,
          directives,
          comments: column.comment === null ? [] : [{
            isError: true,
            text: column.comment
          }]
        } as IGQLField
      })

      const inlineRelations = tc.relations.filter(relation => {
        return relation.source_table === tc.name
      })
      const inlineRelationFields = inlineRelations.map(relation => {
        const ambiguousRelations = tc.relations.filter(innerRelation => innerRelation.source_table === relation.source_table && innerRelation.target_table === relation.target_table)
        const remoteColumns = _.intersectionWith(
          tc.columns,
          ambiguousRelations,
          (a, b) => a.name === b.source_column
        )
        
        const selfAmbiguousRelations = ambiguousRelations.filter(relation => relation.source_table === relation.target_table)
        const selfRemoteColumns = _.intersectionWith(
          tc.columns,
          selfAmbiguousRelations,
          (a, b) => a.name === b.source_column
        )

        const relationName = plural(relation.source_table) + '_' + plural(this.removeIdSuffix(relation.source_column))

        const directives: IDirectiveInfo[] = [{
          name: "pgRelation",
          arguments: {
            column: `"${relation.source_column}"`
          }
        }]

        const isAmbigous = ambiguousRelations.length > 1 && remoteColumns && remoteColumns.length > 0 ||
                         selfAmbiguousRelations.length > 0 && selfRemoteColumns && selfRemoteColumns.length > 0  

        return {
          name: this.removeIdSuffix(relation.source_column),
          type: capitalize(relation.target_table),
          isReadOnly: false,
          isRequired: false,
          isId: false,
          isUnique: false,
          defaultValue: null,
          isList: false,
          relatedField: null, // TODO: Find and link related field, if possible.
          relationName: isAmbigous ? camelCase(relationName) : null,
          directives
        } as IGQLField
      })

      const relationTables = joinTables.reduce((relations, joinTable) => {
        if (joinTable.relations.some(relation => relation.target_table === tc.name)) {
          return relations.concat(joinTable.relations.filter(relation => relation.target_table !== tc.name))
        } else {
          return relations
        }
      }, [] as TableRelation[])

      const relations = tc.relations.filter(relation => {
        return relation.target_table === tc.name &&
              // Join tables are rendered seperately.
              !joinTables.some(x => x.name === relation.target_table)
      })

      const relationFields = relations.map(relation => {
        const ambiguousRelations = tc.relations.filter(innerRelation => innerRelation.source_table === relation.source_table && innerRelation.target_table === relation.target_table)
        const fieldName = ambiguousRelations.length > 1 
                ? plural(relation.source_table) + '_' + plural(
                  this.removeIdSuffix(relation.source_column)
                )
                : plural(relation.source_table)

        const selfAmbiguousRelations = ambiguousRelations.filter(relation => relation.source_table === relation.target_table)

        const isAmbigous = ambiguousRelations.length > 1  || selfAmbiguousRelations.length > 0

        return {
          name: fieldName, 
          type: capitalize(relation.source_table), 
          isRequired: true,
          isReadOnly: false,
          isId: false,
          isList: true, 
          isUnique: false,
          defaultValue: null,
          relatedField: null, // TODO
          relationName: isAmbigous ? camelCase(fieldName) : null
        } as IGQLField
      })

      const relationTableFields = relationTables.map(relation => {

        const directives: IDirectiveInfo[] = [{
          name: "pgRelationTable",
          arguments: {
            table: `"${relation.source_table}`,
            name: `"${relation.source_table}`
          }
        }]
  
        // TODO Include directives
        return {
          name: plural(relation.target_table), 
          isList: true, 
          isRequired: true,
          isId: false, 
          isUnique: false,
          defaultValue: null,
          relatedField: null, // Is this correct? 
          relationName: null,
          isReadOnly: false,
          type: capitalize(relation.target_table)
        } as IGQLField
      })
      
      const allFields = [
        ...(_.differenceWith(fields, inlineRelationFields, (a, b) => {
          return this.removeIdSuffix(a.name) === this.removeIdSuffix(b.name)
        })),
        ...inlineRelationFields,
        ...relationFields,
        ...relationTableFields
      ]

      // TODO: If has zero valid fields, don't render. 
      return {
        name: name,
        fields: allFields,
        isEmbedded: false,
        isEnum: false,
        directives: directives
      } as IGQLType
    })

    return {
      types
    }
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
