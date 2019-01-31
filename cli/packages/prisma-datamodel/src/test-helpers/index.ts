import { IGQLType, IGQLField, IDirectiveInfo } from '../datamodel/model'
import GQLAssert from '../util/gqlAssert';


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

    if(fieldObj === undefined) {
      GQLAssert.raise(`Field ${name} not found. Existing fields: ${candidate.fields.map(f => f.name).join(', ')}`)
    }

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

    if(type === undefined) {
      GQLAssert.raise(`Type ${name} not found. Existing types: ${types.map(t => t.name).join(', ')}`)
    }

    expect(type.isEnum).toEqual(isEnum)
    expect(type.isEmbedded).toEqual(isEmbedded)

    return type
  }

  static index(obj: IGQLType, name: string, fields: IGQLField[], unique: boolean) {
    if(obj.indices === undefined) {
      GQLAssert.raise(`Expected an index on type ${obj.name}, but found none.`)
      return
    }

    const indices = obj.indices.filter(x => x.name === name)

    if(indices.length !== 1) {
      GQLAssert.raise(`Expected exactly one index with name ${name} on type ${obj.name}, but found ${indices.length}.`)
    }

    const index = indices[0]

    expect(index.name).toBe(name)
    expect(index.unique).toBe(unique)
    expect(index.fields.sort()).toEqual(fields.sort())
  }

  /**
   * Assertion helper for directives. 
   */
  static directive(obj: IGQLType | IGQLField, target: IDirectiveInfo) {
    expect(obj.directives).toBeDefined()

    if(obj.directives !== undefined) {
      const [directive] = obj.directives.filter(x => x.name === target.name)
      if(directive === undefined) {
        GQLAssert.raise(`Directive ${target.name} not found. Existing directives: ${obj.directives.map(d => d.name).join(', ')}`)
      }
      expect(directive).toEqual(target)

      return directive
    } else {
      throw new Error('This is a dummy to ensure correct type inferrence.')
    }
  }
}
