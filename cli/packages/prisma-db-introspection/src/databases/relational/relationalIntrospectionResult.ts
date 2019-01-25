import { IntrospectionResult } from '../../common/introspectionResult'
import { ITable, IColumn, IIndex, ITableRelation } from './relationalConnector'
import { ISDL, DatabaseType, Renderer, IGQLField, IGQLType, TypeIdentifier, IIndexInfo, GQLAssert, IComment, camelCase } from 'prisma-datamodel'
import { TypeIdentifiers } from '../../../../prisma-datamodel/dist/datamodel/scalar';

/*
Relational Introspector changes
 [x] Inline relations 1:1 vs 1:n (via unique constraint)
 [ ] Inline relations 1:1 vs 1:n (via data inspection)
 [x] Always add back relations for inline relations
 [x] Correctly handle scalar list tables
 [ ] Add unit test for scalar list table handling 
Postgres introspector changes
 [x] Turn string field with isId into ID field
Renderer changes
 [ ] Remove `@relation` if only one relation from A to B
Normalizer changes
 [ ] db/pgColumn directive not picked up correctly
 [ ] preserve order of fields and types
 [ ] hide createdAt/updatedAt if also hidden in the reference datamodel
 [ ] hide back relations if also hidden in the reference datamodel
 [ ] in v1: handle join tables for 1:n or 1:1 relations correclty, e.g. do no show a n:n relation in this case
 [ ] migrating v1 to v2: In the case above, add a @relation(link: TABLE) directive.
*/

export abstract class RelationalIntrospectionResult extends IntrospectionResult {

  protected model: ITable[]
  protected relations: ITableRelation[]

  constructor(model: ITable[], relations: ITableRelation[], databaseType: DatabaseType, renderer?: Renderer) {
    super(databaseType, renderer)

    this.model = model
    this.relations = relations
  }

  public getDatamodel(): ISDL {
    return this.infer(this.model, this.relations)
  }

  protected resolveRelations(types: IGQLType[], relations: ITableRelation[]) {
    for(const relation of relations) {
      this.resolveRelation(types, relation)
    }
    return types
  }

  private getDatabaseName(obj: IGQLType | IGQLField) {
    if(obj.databaseName !== null) {
      return obj.databaseName
    } else {
      return obj.name
    }
  }

  protected resolveRelation(types: IGQLType[], relation: ITableRelation) { 
    // Correctly sets field types according to given FK constraints.
    for(const typeA of types) {
      for(const typeB of types) {
        for(const fieldA of typeA.fields) {
          for(const fieldB of typeB.fields) {
            if(relation.sourceColumn === this.getDatabaseName(fieldA) && relation.sourceTable === this.getDatabaseName(typeA) &&
              relation.targetColumn === this.getDatabaseName(fieldB) && relation.targetTable === this.getDatabaseName(typeB)) {
              
              if(!fieldB.isId) {
                GQLAssert.raise(`Relation ${typeA.name}.${fieldA.name} -> ${typeB.name}.${fieldB.name} does not target the PK column of ${typeB.name}`)
              }

              fieldA.type = typeB
              
              // TODO: We could look at the data to see if this is 1:1 or 1:n. For now, we use a unique constraint. Tell Tim.

              // Add back connecting field
              const connectorFieldAtB: IGQLField = {
                // TODO - how do we name that field? 
                // Problems: 
                // * Conflicts
                // * Need to identify field in existing datamodel to fix naming, fix cardinality or hide
                name: camelCase(typeA.name),
                databaseName: null,
                defaultValue: null,
                isList: fieldA.isUnique,
                isCreatedAt: false,
                isUpdatedAt: false,
                isId: false,
                isReadOnly: false,
                isRequired: fieldA.isRequired, // TODO: Not sure if that makes sense
                isUnique: false,
                relatedField: fieldA,
                type: typeA,
                relationName: null,
                comments: [],
                directives: []
              }
              
              // Hook up connector fields
              fieldA.relatedField = connectorFieldAtB
              typeB.fields.push(connectorFieldAtB)
              return
            }
          }
        }
      }
    }

    GQLAssert.raise(`Failed to resolve FK constraint ${relation.sourceTable}.${relation.sourceColumn} -> ${relation.targetTable}.${relation.targetColumn}.`)
  }
  
  /**
   * TODO: This is not captured yet by unit tests.
   */
  protected hideScalarListTypes(types: IGQLType[]): IGQLType[] {

    const scalarListTypes: IGQLType[] = []

    for(const type of types) {
      for(const field of type.fields) {
        for(const candidate of types) {
          // A type is only a scalar list iff it has
          // * name of ${type.name}_${field.name}
          // * Has exactly three fields
          //     * nodeId: typeof type!
          //     * position: Int!
          //     * value: ?
          // TODO: Tim mentioned, but not observed in the wild.
          // * compound index over nodeId and position

          if(candidate.fields.length !== 3)
            continue

          if(candidate.name === `${type.name}_${field.name}`)
            continue

          const [nodeId] = candidate.fields.filter(field => 
            field.name === 'nodeId' && 
            field.type === type && 
            field.isRequired == true && 
            field.isList === false)

          const [position] = candidate.fields.filter(field => 
            field.name === 'position' && 
            field.type === TypeIdentifiers.integer && 
            field.isRequired == true && 
            field.isList === false)


          const [value] = candidate.fields.filter(field => 
            field.name === 'value' && 
            field.isRequired == true && 
            field.isList === false)

          if(nodeId === undefined || position === undefined || value === undefined) 
            continue

          // If we got so far, we have found a scalar list type. Hurray!
          scalarListTypes.push(candidate)
          
          // Update the field to show a scalar list
          field.type = value.type
          field.isList = true
        }
      }
    }
    
    // Filter out scalar list types
    return types.filter(type => !scalarListTypes.includes(type))
  }

  /**
   * Removes all types which are only there for NM relations
   * @param types
   */
  protected hideJoinTypes(types: IGQLType[]) {
    const nonJoinTypes: IGQLType[] = []

    for(const type of types) {
      const relationFields = type.fields.filter(field => typeof field.type !== 'string')
      const relationAndIdFields = type.fields.filter(field => typeof field.type !== 'string' || field.isId)

      // A join type only as two relation fields. And sometimes a primary key.
      const isJoinType = (relationFields.length === 2 && type.fields.length === 2) || (relationAndIdFields.length === 3 && type.fields.length === 3)

      if(isJoinType) {
        // We drop the join type, but add a n:m relation to 
        // the two related tables.
        const [relA, relB] = relationFields
        
        // Type checking was done above
        const typeA = relA.type as IGQLType
        const typeB = relB.type as IGQLType

        const relatedFieldForA: IGQLField = {
          name: typeB.name,
          type: typeB,
          isList: true,
          isUnique: false,
          isId: false,
          isCreatedAt: false,
          isUpdatedAt: false,
          isRequired: true,
          isReadOnly: false,
          comments: [],
          directives: [],
          defaultValue: null,
          relatedField: null,
          databaseName: null,
          relationName: type.name
        }

        const relatedFieldForB: IGQLField = {
          name: typeA.name,
          type: typeA,
          isList: true,
          isUnique: false,
          isId: false,
          isCreatedAt: false,
          isUpdatedAt: false,
          isRequired: true,
          isReadOnly: false,
          comments: [],
          directives: [],
          defaultValue: null,
          relatedField: relatedFieldForA,
          databaseName: null,
          relationName: type.name
        }

        relatedFieldForA.relatedField = relatedFieldForB

        typeA.fields.push(relatedFieldForA)
        typeA.fields = typeA.fields.filter(x => x.type !== type)
        typeB.fields = typeB.fields.filter(x => x.type !== type)
        typeB.fields.push(relatedFieldForB)
      } else {
        nonJoinTypes.push(type)
      }
    }

    return nonJoinTypes
  }

  /**
   * Hides indices on related fields. These are always autmatically created by 
   * prisma and dont need to be shown in the datamodel.
   * @param types 
   */
  protected hideIndicesOnRelatedFields(types: IGQLType[]) {
    for(const type of types) {
      // Keep indices which have not exactly one field, or if they have one field, 
      // the field is a scalar field.
      type.indices = type.indices.filter(index => 
        index.fields.length !== 1 ||
        typeof index.fields[0].type === 'string')
    }
    return types
  }

  protected abstract isTypeReserved(type: IGQLType): boolean

  protected hideReservedTypes(types: IGQLType[]) {
    return types.filter(x => !this.isTypeReserved(x))
  }

  protected infer(model: ITable[], relations: ITableRelation[]): ISDL {
    // TODO: Enums? 

    // TODO: Maybe we want to have a concept of hidden, which just skips rendering?
    // Ask tim, this is an important descision for the SDK
    let types = model.map(x => this.inferObjectType(x))
    types = this.resolveRelations(types, relations)
    types = this.hideJoinTypes(types)
    types = this.hideReservedTypes(types)
    types = this.hideIndicesOnRelatedFields(types)
    types = this.hideJoinTypes(types)

    return {
      comments: [],
      types: types
    }
  }

  protected inferIndex(index: IIndex, fields: IGQLField[]) : IIndexInfo {
    const fieldCandidates = fields.filter(field => index.fields.filter(indexField => field.name === indexField).length > 0)
    
    return {
      fields: fieldCandidates,
      name: index.name,
      unique: index.unique
    }
  }

  protected inferObjectType(model: ITable): IGQLType {

    const fields = model.columns.map(x => this.inferField(x))
    const indices = model.indices.map(x => this.inferIndex(x, fields))

    // Resolve primary key
    if(model.primaryKey !== null) {
      const pk = model.primaryKey
      if(pk.fields.length === 1) {
        // Single PK field - prisma can do that
        const [pkField] = fields.filter(field => field.name === pk.fields[0])
        if(!pkField) {
          GQLAssert.raise(`Index/Schema missmatch during introspection. Field ${pk.fields[0]} used in index, but does not exist on table ${model.name}`)
        }
        // Hard rename ID field to match old datamodel standard
        if(pkField.name !== 'id') {
          pkField.databaseName = pkField.name
          pkField.name = 'id'
        }
        pkField.isId = true
      } else {
        // Compound PK - that's not supported
        for(const pkFieldName of pk.fields) {
          const [pkField] = fields.filter(field => field.name === pk.fields[0])
          if(!pkField) {
            GQLAssert.raise(`Index/Schema missmatch during introspection. Field ${pk.fields[0]} used in index, but does not exist on table ${model.name}`)
          }
          pkField.isId = true
          pkField.comments.push({
            text: `Multiple ID fields (compound indices) are not supported`,
            isError: true
          })
        }
      }
    }

    // We need info about indices for resolving the exact type, as String is mapped to ID.
    // Also, we need info about the actual type before we resolve default values.
    for(const field of fields) {
      this.inferFieldTypeAndDefaultValue(field)
    }

    return {
      name: model.name,
      isEmbedded: false, // Never
      isEnum: false, // Never
      fields,
      indices,
      directives: [],
      comments: [],
      databaseName: null
    }
  }

  protected inferField(field: IColumn): IGQLField {

    const comments: IComment[] = []

    if(field.comment !== null) {
      comments.push({
        text: field.comment,
        isError: false
      })
    }

    const gqlField: IGQLField = {
      name: field.name,
      isUnique: field.isUnique,
      isRequired: !field.isNullable,
      defaultValue: field.defaultValue,
      isList: field.isList,
      type: field.type,
      isId: false, // Will resolve later, from indices
      relatedField: null,
      relationName: null,
      isCreatedAt: false, // No longer depends on the DB
      isUpdatedAt: false, // No longer depends on the DB
      isReadOnly: false, // Never
      comments,
      directives: [],
      databaseName: null
    }

    return gqlField
  }

  protected inferFieldTypeAndDefaultValue(field: IGQLField) {
    GQLAssert.raiseIf(typeof field.type !== 'string', 'Must be called before resolving relations')
    let type: string | null = this.toTypeIdentifyer(field.type as string, field)

    if(type === null) {
      field.comments.push({
        text: `Type ${field.type} is not supported`,
        isError: true
      })
      // Keep native type and register an error.
    } else {
      field.type = type
    }

    if(field.defaultValue !== null) {
      GQLAssert.raiseIf(typeof field.defaultValue !== 'string', 'Must be called with unparsed default values.')
      field.defaultValue = this.parseDefaultValue(field.defaultValue as string, field.type as string)
    }
  }

  /**
   * Maps a native database type. If null is returned, the corresponding
   * field is marked with an error comment.
   * @param typeName 
   */
  protected abstract toTypeIdentifyer(typeName: string, fieldInfo: IGQLField): TypeIdentifier | null

  protected abstract parseDefaultValue(defaultValueString: string, type: string): string | null
}