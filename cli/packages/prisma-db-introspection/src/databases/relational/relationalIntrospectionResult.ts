import { IntrospectionResult } from '../../common/introspectionResult'
import { ITable, IColumn, IIndex, ITableRelation } from './relationalConnector'
import { ISDL, DatabaseType, Renderer, IGQLField, IGQLType, TypeIdentifier, IIndexInfo, GQLAssert, IComment, camelCase } from 'prisma-datamodel'

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
              return
            }
          }
        }
      }
    }

    GQLAssert.raise(`Failed to resolve FK constraint ${relation.sourceTable}.${relation.sourceColumn} -> ${relation.targetTable}.${relation.targetColumn}.`)
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

    let types = model.map(x => this.inferObjectType(x))
    types = this.resolveRelations(types, relations)
    types = this.hideJoinTypes(types)
    types = this.hideReservedTypes(types)
    types = this.hideIndicesOnRelatedFields(types)

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

    let type: string | null = this.toTypeIdentifyer(field.type)

    if(type === null) {
      comments.push({
        text: `Type ${field.type} is not supported`,
        isError: true
      })
      type = field.type
    }

    return {
      name: field.name,
      isUnique: field.isUnique,
      isRequired: !field.isNullable,
      defaultValue: field.defaultValue !== null ? this.parseDefaultValue(field.defaultValue, type) : null,
      isList: field.isList,
      type: type as string,
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
  }

  /**
   * Maps a native database type. If null is returned, the corresponding
   * field is marked with an error comment.
   * @param typeName 
   */
  protected abstract toTypeIdentifyer(typeName: string): TypeIdentifier | null

  protected abstract parseDefaultValue(defaultValueString: string, type: string): string | null
}