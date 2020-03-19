import {
  ITable,
  ITableRelation,
  IEnum,
  ISequenceInfo,
} from '../relationalConnector'
import { RelationalIntrospectionResult } from '../relationalIntrospectionResult'
import {
  IGQLField,
  IGQLType,
  capitalize,
  DatabaseType,
  Renderer,
  TypeIdentifiers,
  IdStrategy,
} from 'prisma-datamodel'

export class MysqlIntrospectionResult extends RelationalIntrospectionResult {
  constructor(
    model: ITable[],
    relations: ITableRelation[],
    enums: IEnum[],
    sequences: ISequenceInfo[],
    renderer?: Renderer,
  ) {
    super(model, relations, enums, sequences, DatabaseType.mysql, renderer)
  }

  protected isTypeReserved(type: IGQLType): boolean {
    return type.name == '_RelayId'
  }
  protected toTypeIdentifyer(
    fieldTypeName: string,
    fieldInfo: IGQLField,
    typeName: string,
  ): string | null {
    const precisionStart = fieldTypeName.indexOf('(')
    const precisionEnd = fieldTypeName.lastIndexOf(')')

    let type = fieldTypeName
    let precision = ''

    if (precisionEnd !== -1 && precisionEnd !== -1) {
      type = fieldTypeName.substring(0, precisionStart)
      precision = fieldTypeName.substring(precisionStart + 1, precisionEnd)
    }

    switch (type) {
      case 'int':
      case 'smallint':
        return TypeIdentifiers.integer
      case 'bigint':
        return TypeIdentifiers.bigInt
      case 'decimal':
      case 'float':
      case 'double':
        return TypeIdentifiers.float
      case 'varchar':
      case 'char':
      case 'longtext':
      case 'mediumtext':
      case 'blob':
      case 'text':
      case 'mediumblob':
      // If we have a text type on an ID field, we map to the ID type.
      case 'smallblob':
        return fieldInfo.isId ? TypeIdentifiers.id : TypeIdentifiers.string
      case 'bool':
      case 'tinyint':
      case 'bit':
        return TypeIdentifiers.boolean
      case 'json':
        return TypeIdentifiers.json
      case 'date':
      case 'time':
      case 'datetime':
      case 'timestamp':
        return TypeIdentifiers.dateTime
      case 'uuid':
        return TypeIdentifiers.uuid
      // Special case: For enum types we auto-generate some unique name.
      case 'enum':
        return capitalize(typeName) + capitalize(fieldInfo.name) + 'Enum'
      default:
        return null
    }
  }
  protected parseDefaultValue(
    defaultValueString: string,
    type: string,
  ): string | null {
    return defaultValueString
  }

  protected resolveSequences(types: IGQLType[], sequences: ISequenceInfo[]) {
    return types // Mysql cannot do that.
  }
}
