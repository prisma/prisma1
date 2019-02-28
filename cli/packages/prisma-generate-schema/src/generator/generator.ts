import {
  GraphQLScalarType,
  GraphQLType,
  GraphQLInterfaceType,
  GraphQLSchema,
  GraphQLFieldConfigMap,
  GraphQLInputObjectType,
  GraphQLFieldConfig,
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLInputFieldConfig,
  GraphQLFieldConfigArgumentMap,
  GraphQLEnumType,
  GraphQLEnumValueConfig,
  GraphQLEnumValueConfigMap,
} from 'graphql/type'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { GraphQLList, GraphQLNonNull } from 'graphql'

// tslint:disable:max-classes-per-file

/**
 * Type cache for object types.
 */
export class TypeRegistry {
  [typeName: string]: GraphQLType
}

/**
 * Base class of all generators,
 * has a reference to the set of generators we need
 * and a type registry.
 */
export abstract class Generator<In, Args, Out> {
  protected knownTypes: TypeRegistry
  protected generators: IGenerators

  constructor(knownTypes: TypeRegistry, generators: IGenerators) {
    this.knownTypes = knownTypes
    this.generators = generators
  }

  /**
   * Generates the thing this generator is generating.
   * @param model
   * @param args
   */
  public abstract generate(model: In, args: Args): Out
}

/**
 * Base class for all generators that create types.
 * This class implements caching via the given TypeRegistry.
 */
export abstract class TypeGenerator<
  In,
  Args,
  Type extends GraphQLType
> extends Generator<In, Args, Type> {
  public abstract getTypeName(input: In, args: Args)

  public generate(input: In, args: Args): Type {
    const name = this.getTypeName(input, args)

    if (this.knownTypes.hasOwnProperty(name)) {
      // Force cast should be safe because of the name lookup.
      return this.knownTypes[name] as Type
    } else {
      const type = this.generateInternal(input, args)
      this.knownTypes[name] = type
      return type
    }
  }

  protected abstract generateInternal(input: In, args?: Args): Type
}

/**
 * Base class for all generators which map
 * a type from the datamodel to some object type.
 *
 * This class adds provides methods that assemble
 * a GraphQLObject type, for code re-use.
 *
 * Ideally a deriving class would override the
 * generateScalarFieldType or generateRelationFieldType methods.
 */
export abstract class TypeFromModelGenerator<
  Args,
  Type extends GraphQLType,
  FieldConfig extends
    | GraphQLFieldConfig<any, any>
    | GraphQLEnumValueConfig
    | GraphQLInputFieldConfig,
  FieldConfigMap extends
    | GraphQLFieldConfigMap<any, any>
    | GraphQLEnumValueConfigMap
    | GraphQLInputFieldConfigMap
> extends TypeGenerator<IGQLType, Args, Type> {
  /**
   * Checks if the given list of fields has
   * a unique field.
   * @param fields
   */
  public hasUniqueField(fields: IGQLField[]) {
    return fields.filter(field => field.isUnique).length > 0
  }

  /**
   * Checks if the given list of fields has
   * other fields than the fields given in the second
   * parameter.
   * @param fields
   * @param fieldNames
   */
  public hasFieldsExcept(fields: IGQLField[], ...fieldNames: string[]) {
    return fields.filter(field => !fieldNames.includes(field.name)).length > 0
  }

  /**
   * Returns all writeable fields in the given field list.
   * @param fields
   */
  public getWriteableFields(fields: IGQLField[]) {
    return fields.filter(field => !field.isReadOnly)
  }

  /**
   * Checks if the given field list contains at least one writeable field.
   */
  public hasWriteableFields(fields: IGQLField[]) {
    return this.getWriteableFields(fields).length > 0
  }

  /**
   * Checks if the given list of fields contains at least one scalar field.
   * @param fields
   */
  public hasScalarFields(fields: IGQLField[]) {
    return this.getScalarFields(fields).length > 0
  }

  /**
   * Returns all scalar fields from the given field list.
   * @param fields
   */
  public getScalarFields(fields: IGQLField[]) {
    return fields.filter(field =>
      this.generators.scalarTypeGenerator.isScalarField(field),
    )
  }

  /**
   * Returns all scalar fields from the given field list.
   * @param fields
   */
  public getRelationFields(fields: IGQLField[]) {
    return fields.filter(
      field => !this.generators.scalarTypeGenerator.isScalarField(field),
    )
  }

  /**
   * Indicates if the resulting type would be empty.
   * @param model
   * @param args
   */
  public wouldBeEmpty(model: IGQLType, args: Args): boolean {
    return false
  }

  /**
   * Generates all fields of this type.
   * @param model
   * @param args
   */
  protected generateFields(model: IGQLType, args: Args): FieldConfigMap {
    const fields = {} as FieldConfigMap

    for (const field of model.fields) {
      const fieldSchema: FieldConfig | null = this.generators.scalarTypeGenerator.isScalarField(
        field,
      )
        ? this.generateScalarField(model, args, field)
        : this.generateRelationField(model, args, field)

      if (fieldSchema !== null) {
        fields[field.name] = fieldSchema
      }
    }

    return fields
  }

  /**
   * Responsible to instantiate the correct GraphQL type
   * for building the ast.
   * @param name
   * @param fields
   */
  protected abstract instantiateObjectType(
    name: string,
    fields: () => FieldConfigMap,
  )

  /**
   * Calls generateFields and wraps the result into a function.
   * Then calls instantiateObjectType to create the actual AST node.
   * @param model
   * @param args
   */
  protected generateInternal(model: IGQLType, args: Args): Type {
    const fieldFunction = () => this.generateFields(model, args)

    return this.instantiateObjectType(
      this.getTypeName(model, args),
      fieldFunction,
    )
  }

  protected generateScalarField(
    model: IGQLType,
    args: Args,
    field: IGQLField,
  ): FieldConfig | null {
    const type = this.generateScalarFieldType(model, args, field)
    if (type === null) {
      return null
    } else {
      // We need a force-cast with any here, since we would need a type constraint for a type that depends on
      // FieldConfig, which is something that TS cannot do.
      return ({ type } as any) as FieldConfig
    }
  }

  protected generateRelationField(
    model: IGQLType,
    args: Args,
    field: IGQLField,
  ): FieldConfig | null {
    const type = this.generateRelationFieldType(model, args, field)
    if (type === null) {
      return null
    } else {
      // We need a force-cast with any here, since we would need a type constraint for a type that depends on
      // FieldConfig, which is something that TS cannot do.
      return ({ type } as any) as FieldConfig
    }
  }

  protected generateScalarFieldType(
    model: IGQLType,
    args: Args,
    field: IGQLField,
  ): GraphQLType | null {
    return this.generators.scalarTypeGenerator.mapToScalarFieldType(field)
  }

  protected generateRelationFieldType(
    model: IGQLType,
    args: Args,
    field: IGQLField,
  ): GraphQLType | null {
    throw new Error('Method not implemented.')
  }
}

/**
 * Base class for all generators that generate GraphQLEnums.
 */
export abstract class ModelEnumTypeGeneratorBase extends TypeFromModelGenerator<
  {},
  GraphQLEnumType,
  GraphQLEnumValueConfig,
  GraphQLEnumValueConfigMap
> {
  protected instantiateObjectType(
    name: string,
    values: () => GraphQLEnumValueConfigMap,
  ) {
    return new GraphQLEnumType({
      name,
      values: values(),
    })
  }
}

/**
 * Base class for all generators that generate GraphQLObjectTypes.
 */
export abstract class ModelObjectTypeGeneratorBase<
  Args
> extends TypeFromModelGenerator<
  Args,
  GraphQLObjectType,
  GraphQLFieldConfig<any, any>,
  GraphQLFieldConfigMap<any, any>
> {
  protected instantiateObjectType(
    name: string,
    fields: () => GraphQLFieldConfigMap<any, any>,
  ) {
    return new GraphQLObjectType({
      name,
      fields,
    })
  }
}
export abstract class ModelObjectTypeGenerator extends ModelObjectTypeGeneratorBase<{}> {}

/**
 * Base class for all generators that generate GraphQLInputObjectTypes.
 */
export abstract class ModelInputObjectTypeGeneratorBase<
  Args
> extends TypeFromModelGenerator<
  Args,
  GraphQLInputObjectType,
  GraphQLInputFieldConfig,
  GraphQLInputFieldConfigMap
> {
  protected instantiateObjectType(
    name: string,
    fields: () => GraphQLInputFieldConfigMap,
  ) {
    return new GraphQLInputObjectType({
      name,
      fields,
    })
  }
}
export abstract class ModelInputObjectTypeGenerator extends ModelInputObjectTypeGeneratorBase<{}> {}

/**
 * Special base class for the scalar field generator.
 */
export abstract class ScalarTypeGeneratorBase extends TypeGenerator<
  string | IGQLType,
  {},
  GraphQLScalarType
> {
  abstract isScalarField(field: IGQLField): boolean
  /**
   * Maps a field to the scalar field type for output objects.
   * @param field
   */
  abstract mapToScalarFieldType(field: IGQLField): GraphQLType
  /**
   * Maps a field to the scalar field type for input objects.
   * @param field
   */
  abstract mapToScalarFieldTypeForInput(field: IGQLField): GraphQLType
  /**
   * Maps a field to the scalar field type, forces the field to be not nullable.
   * @param field
   */
  abstract mapToScalarFieldTypeForceRequired(field: IGQLField): GraphQLType
  /**
   * Maps a field to the scalar field type, forces the field to be nullable.
   * @param field
   */
  abstract mapToScalarFieldTypeForceOptional(field: IGQLField): GraphQLType
  /**
   * Transforms a given GraphQLScalarType into a list of the given type, according
   * to the OpenCRUD spec.
   * @param field
   */
  abstract wrapList<T extends GraphQLType>(
    field: T,
  ): GraphQLList<GraphQLNonNull<T>>
  abstract requiredIf<T extends GraphQLType>(
    condition: boolean,
    field: T,
  ): T | GraphQLNonNull<T>
  abstract wraphWithModifiers<T extends GraphQLType>(
    field: IGQLField,
    type: T,
  ): T | GraphQLList<GraphQLNonNull<T>> | GraphQLNonNull<T>
}

/**
 * Abstract base class for all generators that generate scalar input fields.
 */
export abstract class ScalarInputGenerator extends TypeGenerator<
  IGQLType,
  IGQLField,
  GraphQLObjectType
> {}

/**
 * Base class for generators that generate argument lists.
 */
export abstract class ArgumentsGenerator extends Generator<
  IGQLType,
  {},
  GraphQLFieldConfigArgumentMap
> {
  public wouldBeEmpty(model: IGQLType, args: {}): boolean {
    return false
  }
}

/**
 * Arguments passed to generators that need to take a related field into account.
 */
export class RelatedGeneratorArgs {
  relatedField: IGQLField
  relatedType: IGQLType
  relationName: string | null
}

/**
 * Base class for generators that generate GraphQLObject types which take a related field into account.
 */
export abstract class RelatedModelInputObjectTypeGenerator extends ModelInputObjectTypeGeneratorBase<
  RelatedGeneratorArgs
> {}

/**
 * Base class for generators that generate a GraphQLObjectType without taking any input.
 */
export abstract class AuxillaryObjectTypeGenerator extends TypeGenerator<
  null,
  {},
  GraphQLObjectType
> {}

/**
 * Base class for generators that generate a GraphQLInterfaceType without taking any input.
 */
export abstract class AuxillaryInterfaceGenerator extends TypeGenerator<
  null,
  {},
  GraphQLInterfaceType
> {}

/**
 * Base class for generators that generate a GraphQLInputObjectType without taking any input.
 */
export abstract class AuxillaryInputObjectTypeGenerator extends TypeGenerator<
  null,
  {},
  GraphQLInputObjectType
> {}

/**
 * Base class for generators that generate a GraphQLEnumType without taking any input.
 */
export abstract class AuxillaryEnumGenerator extends TypeGenerator<
  null,
  {},
  GraphQLEnumType
> {}

/**
 * Base class for generators that generate a query, mutation or subscription object from
 * a list of datamodel types.
 */
export abstract class RootGenerator extends TypeGenerator<
  IGQLType[],
  {},
  GraphQLObjectType
> {}

/**
 * Base class for generators that generate a schema from a list of datamodel types.
 */
export abstract class SchemaGeneratorBase extends Generator<
  IGQLType[],
  {},
  GraphQLSchema
> {}

/**
 * Base class specifying a list of generators to implement.
 */
export interface IGenerators {
  // Create
  modelCreateInput: ModelInputObjectTypeGenerator
  modelCreateOneInput: ModelInputObjectTypeGenerator
  modelCreateManyInput: ModelInputObjectTypeGenerator
  modelCreateWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelCreateOneWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelCreateManyWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  scalarListCreateInput: ScalarInputGenerator

  // Update
  modelUpdateInput: ModelInputObjectTypeGenerator
  modelUpdateDataInput: ModelInputObjectTypeGenerator
  modelUpdateManyDataInput: ModelInputObjectTypeGenerator
  modelUpdateOneInput: ModelInputObjectTypeGenerator
  modelUpdateOneRequiredInput: ModelInputObjectTypeGenerator
  modelUpdateManyInput: ModelInputObjectTypeGenerator
  modelUpdateManyMutationInput: ModelInputObjectTypeGenerator
  modelUpdateWithoutRelatedDataInput: RelatedModelInputObjectTypeGenerator
  modelUpdateOneWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelUpdateOneRequiredWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelUpdateManyWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  scalarListUpdateInput: ScalarInputGenerator

  modelUpdateWithWhereUniqueWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelUpdateWithWhereUniqueNestedInput: ModelInputObjectTypeGenerator
  modelUpdateManyWithWhereNestedInput: ModelInputObjectTypeGenerator

  // Upsert
  modelUpsertNestedInput: ModelInputObjectTypeGenerator
  modelUpsertWithWhereUniqueWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelUpsertWithoutRelatedInput: RelatedModelInputObjectTypeGenerator
  modelUpsertWithWhereUniqueNestedInput: ModelInputObjectTypeGenerator

  // Deleting uses ModelWhereInput

  // Querying
  modelWhereUniqueInput: ModelInputObjectTypeGenerator
  modelScalarWhereInput: ModelInputObjectTypeGenerator
  modelWhereInput: ModelInputObjectTypeGenerator
  modelRestrictedWhereInput: ModelInputObjectTypeGenerator
  modelOrderByInput: ModelEnumTypeGeneratorBase
  modelConnection: ModelObjectTypeGenerator
  modelEdge: ModelObjectTypeGenerator
  aggregateModel: ModelObjectTypeGenerator
  pageInfo: AuxillaryObjectTypeGenerator
  model: ModelObjectTypeGenerator
  oneQueryArguments: ArgumentsGenerator
  manyQueryArguments: ArgumentsGenerator
  uniqueQueryArguments: ArgumentsGenerator
  node: AuxillaryInterfaceGenerator

  // Auxillary Types
  batchPayload: AuxillaryObjectTypeGenerator

  // Subscription
  modelSubscriptionPayload: ModelObjectTypeGenerator
  modelSubscriptionWhereInput: ModelInputObjectTypeGenerator
  mutationType: AuxillaryEnumGenerator
  modelPreviousValues: ModelObjectTypeGenerator

  // Root
  query: RootGenerator
  mutation: RootGenerator
  subscription: RootGenerator
  schema: SchemaGeneratorBase

  // Scalar
  modelEnumTypeGenerator: ModelEnumTypeGeneratorBase
  scalarTypeGenerator: ScalarTypeGeneratorBase
}

/**
 * Utility class that merges field configration.
 */
export class FieldConfigUtils {
  /**
   * Merges all given field config maps.
   * @param fieldMaps The field config maps to merge.
   */
  public static merge<
    T extends GraphQLFieldConfigMap<any, any> | GraphQLInputFieldConfigMap
  >(...fieldMaps: T[]): T {
    const newMap = {} as T

    for (const fieldMap of fieldMaps) {
      if (fieldMap === null) {
        continue
      }

      Object.keys(fieldMap).forEach((name: string) => {
        const field = fieldMap[name]
        if (name in newMap) {
          console.dir(fieldMaps)
          throw new Error(
            'Field configuration to merge has duplicate field names.',
          )
        }
        newMap[name] = field
      })
    }

    return newMap
  }
}
