import {
  GraphQLScalarType,
  GraphQLNonNull,
  GraphQLList,
  GraphQLObjectType,
  GraphQLType,
} from 'graphql'
import {
  GraphQLString,
  GraphQLInt,
  GraphQLFloat,
  GraphQLBoolean,
  GraphQLID,
} from 'graphql/type'

// tslint:disable:max-classes-per-file

/**
 * Represents a list of name, value pairs
 * to represent arguments.
 */
export interface IArguments {
  [name: string]: string
}

/**
 * Represents a directive
 */
export interface IDirectiveInfo {
  name: string
  arguments: IArguments
}

/**
 * Represents a comment.
 * If the error flag is set, the comment indicates an error
 * and should be trated accordingly.
 */
export interface IComment {
  text: string
  isError: boolean
}

/**
 * Represents a field in the datamodel.
 */
export interface IGQLField {
  /**
   * The name of this sfield.
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

  /**
   * Indicates this fields extra directives, 
   * which can not expressed using this 
   * interface's other members.
   */
  directives?: IDirectiveInfo[]

  /**
   * Comments for this field.
   */
  comments?: IComment[]
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

  /**
   * Indicates this types extra directives, 
   * which can not expressed using this 
   * interface's other members.
   */
  directives?: IDirectiveInfo[]

  /**
   * Comments for this type.
   */
  comments?: IComment[]
}

export interface ISDL {
  /**
   * All types in this datamodel.
   */
  types: IGQLType[] 

  /**
   * Comments for this datamodel.
   */
  comments?: IComment[]
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
  public defaultValue: any
  public isId: boolean
  public isReadOnly: boolean
  public directives?: IDirectiveInfo[]
  public comments?: IComment[]

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

    this.directives = []
    this.comments = []
  }
}

export class GQLScalarField extends GQLFieldBase {
  constructor(name: string, type: string | IGQLType, isRequired?: boolean) {
    super(name, type, isRequired)
  }
}
export class GQLOneRelationField extends GQLFieldBase {
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
