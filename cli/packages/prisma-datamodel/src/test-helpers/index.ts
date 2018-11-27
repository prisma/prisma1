import { IGQLType, IGQLField } from '../datamodel/model'


export abstract class SdlExpect {
  /**
   * Assertion helper for fields.
   */
  static field(
    candidate: IGQLType,
    name: string,
    required: boolean,
    list: boolean,
    type: string | IGQLType,
    isId: boolean = false,
    isReadOnly: boolean = false,
    defaultValue: any = null
  ) : IGQLField {
    const [fieldObj] = candidate.fields.filter(f => f.name === name)

    expect(fieldObj).toBeDefined()

    expect(fieldObj.isRequired).toEqual(required)
    expect(fieldObj.isList).toEqual(list)
    expect(fieldObj.type).toEqual(type)
    expect(fieldObj.defaultValue).toEqual(defaultValue)
    expect(fieldObj.isId).toEqual(isId)
    expect(fieldObj.isReadOnly).toEqual(isReadOnly)
    expect(fieldObj.defaultValue).toEqual(defaultValue)

    return fieldObj
  }

  /**
   * Assertion helper for errors.
   */
  static error(object: IGQLType | IGQLField) {
    expect(object.comments).toBeDefined()
    if(object.comments !== undefined) {
      expect(object.comments.some(x => x.isError))
    }
  }

  /**
   * Assertion helper for types
   */
  static type(types: IGQLType[], name: string, isEnum: boolean = false, isEmbedded: boolean = false) : IGQLType {
    const [type] = types.filter(t => t.name === name)

    expect(type).toBeDefined()
    expect(type.isEnum).toEqual(isEnum)
    expect(type.isEmbedded).toEqual(isEmbedded)

    return type
  }
}
