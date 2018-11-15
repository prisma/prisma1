import { GraphQLScalarType, GraphQLNonNull, GraphQLList, GraphQLObjectType, GraphQLType } from "graphql"
import { GraphQLString, GraphQLInt, GraphQLFloat, GraphQLBoolean, GraphQLID } from 'graphql/type'

// tslint:disable:max-classes-per-file

/**
 * Represents a field in the datamodel.
 */
export interface IGQLField {
  /**
   * The name of thi sfield. 
   */
  name: string
  /**
   * The type of this field. A value of 
   * type string indicates a scalar type.
   */
  type: string | IGQLType
  /**
   * Indicates if this field is required.
   */
  isRequired: boolean
  /**
   * Inidicates if this field holds
   * a list of values. Can be set for
   * scalars or relations. 
   */
  isList: boolean
  /**
   * The default value of a given field,
   * if any.
   */
  defaultValue: string | number | null
  /**
   * This is set for double-sided relations.
   */
  relatedField: IGQLField | null
  /**
   * This is only set for relations with a name given via 
   * an directive. 
   */
  relationName: string | null
  /**
   * Indicates if this field is uniqe across the model. 
   */
  isUnique: boolean

  /**
   * Indicates if this field is the unique identifyer.
   */
  isId: boolean

  /**
   * Indicates if this field is read-only. 
   */
  isReadOnly: boolean
}

/**
 * Represents a type in the datamodel.
 */
export interface IGQLType {
  /**
   * Indicates if this is an embedded type.
   */
  isEmbedded: boolean
  
  /**
   * Indicates if this is an enum type.
   */
  isEnum: boolean
  /**
   * The name of this type.
   */
  name: string
  /**
   * A list of all fields of this type.
   * 
   * If this is an enum type, only the name properties of each
   * field are relevant.
   */
  fields: IGQLField[]
}

/**
 * Internal manifestations of the interfaces declared above. 
 */
export class GQLFieldBase implements IGQLField {
  public name: string
  public type: string | IGQLType
  public isRequired: boolean
  public isList: boolean
  public relatedField: IGQLField | null
  public relationName: string | null
  public isUnique: boolean
  public defaultValue: null
  public isId: boolean
  public isReadOnly: boolean

  constructor(name: string, type: IGQLType | string, isRequired?: boolean) {
    this.name = name
    this.type = type
    this.isRequired = isRequired || false
    this.isList = false
    this.relatedField = null
    this.relationName = null
    this.isUnique = false
    this.defaultValue = null
    this.isId = false
    this.isReadOnly = false
  }
}

export class GQLScalarField extends GQLFieldBase {
  constructor(name: string, type: string | IGQLType, isRequired?: boolean) {
    super(name, type, isRequired)
  }
}
export class GQLOneRelationField extends GQLFieldBase  {
  constructor(name: string, type: IGQLType, isRequired?: boolean) {
    super(name, type, isRequired)
  }
}

// TODO: Create abstract base class for testing. 
export class GQLMultiRelationField extends GQLFieldBase {
  constructor(name: string, type: IGQLType, isRequired?: boolean) {
    super(name, type, isRequired)
    this.isList = true
  }
}