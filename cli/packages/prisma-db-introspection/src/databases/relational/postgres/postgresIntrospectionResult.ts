import { ITable, ITableRelation } from "../relationalConnector"
import { RelationalIntrospectionResult } from "../relationalIntrospectionResult"
import { ISDL, IGQLField, IGQLType, IDirectiveInfo, plural, camelCase, capitalize, DatabaseType, Renderer, TypeIdentifier, TypeIdentifiers } from 'prisma-datamodel'
import * as _ from 'lodash'

export class PostgresIntrospectionResult extends RelationalIntrospectionResult {
  constructor(model: ITable[], relations: ITableRelation[], renderer?: Renderer) {
    super(model, relations, DatabaseType.postgres, renderer)
  }

  protected isTypeReserved(type: IGQLType): boolean {
    return type.name == '_RelayId'
  }
  protected toTypeIdentifyer(typeName: string, fieldInfo: IGQLField): TypeIdentifier | null {
    switch(typeName) {
      case 'int1':
      case 'int2':
      case 'int4': 
      case 'int8': return TypeIdentifiers.integer
      case 'numeric':
      case 'float4':
      case 'float8': return TypeIdentifiers.float
      case 'varchar':
      case 'bpchar':
      case '_text':
      // If we have a text type on an ID field, we map to the ID type.
      case 'text': return fieldInfo.isId ? TypeIdentifiers.id : TypeIdentifiers.string
      case 'bool': return TypeIdentifiers.boolean
      case 'jsonb':
      case 'json': return TypeIdentifiers.json
      case '_date':
      case 'date':
      case 'timestamp': return TypeIdentifiers.dateTime
      case 'uuid': return TypeIdentifiers.uuid
      default: return null
    }
  }
  protected parseDefaultValue(defaultValueString: string, type: string): string | null {
    let val = defaultValueString

    // Detect string
    if(val.startsWith('\'')) {
      // Strip quotes (are added again by renderer)
      val = val.substring(1, val.length - 1)
    }

    // Remove cast operator
    const i = val.indexOf('::')

    if(i >= 0) {
      val = val.substring(0, i)
    }

    // Check for null
    if(val.toUpperCase() === 'NULL') {
      return null
    }
    
    // If the field is not a string field, 
    // and the default val is not a boolean or a number, we assume a function call or sequence reference. 
    if(type !== TypeIdentifiers.string && type != TypeIdentifiers.id) {
      if(isNaN(val as any) && val.toLowerCase() !== 'true' && val.toLowerCase() !== 'false') {
        return null
      }
    }

    // TODO: Sequences are simply ignored.

    return val
  }

}
