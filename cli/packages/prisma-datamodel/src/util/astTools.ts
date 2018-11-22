import { GraphQLSchema, isEnumType, isObjectType, isInputObjectType, isScalarType, isInterfaceType } from 'graphql/type'
import GQLAssert from './gqlAssert';

export default abstract class AstTools {
  
  /**
   * Checks if two schemas are equal.
   * 
   * A schema A is considered equal to schema B if: 
   *  * A has exactly the same types as B.
   *  * For field, the type, name and arguments match exactly. 
   * @param original 
   * @param toCheck 
   */
  public static assertTypesEqual(original : GraphQLSchema, toCheck: GraphQLSchema, modelName: string) {

    const typeMapA = original.getTypeMap()
    const typeMapB = toCheck.getTypeMap()

    const errors = AstTools.allTypesFromAInB(typeMapA, typeMapB, true) + AstTools.allTypesFromAInB(typeMapB, typeMapA, false)
    
    if(errors > 0) {
      console.error(`${errors} type checking errors. Type equality check for model ${modelName} failed.`)
      GQLAssert.raise(`${errors} type checking errors. Type equality check for model ${modelName} failed.`)
    }
  }

  private static allTypesFromAInB(typeMapA : any, typeMapB: any, aIsOriginal : boolean) {
    let errors = 0
    for(const typeName of Object.keys(typeMapA)) {
      const bType = typeMapB[typeName]

      if(bType === undefined && typeName === 'Json') {
        continue // TODO: We are generating Json only if it is used. Sync Prisma and generation
      }

      if(bType === undefined) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`Type ${typeName} is missing.`)
        } else {
          console.error(`Type ${typeName} is extra.`)
        }
        continue // Deeper check has no meaning. 
      }
      
      const aType = typeMapA[typeName]

      if(aType.constructor.name !== bType.constructor.name) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`Type ${typeName} should be ${aType.constructor.name}, was ${bType.constructor.name}.`)
        } else {
          console.error(`Type ${typeName} should be ${typeof(bType.constructor.name)}, was ${aType.constructor.name}.`)
        }
        continue // Deeper check has no meaning. 
      }

      if(isEnumType(aType)) {
        errors = errors + AstTools.allEnumFieldsFromAInB(aType, bType, aIsOriginal)
      } else if(isObjectType(aType) || isInputObjectType(aType) || isInterfaceType(aType)) {
        errors = errors + AstTools.allFieldsFromAInB(aType, bType, aIsOriginal)
      } else if(isScalarType(aType)) {
        errors = errors + AstTools.scalarTypeEquals(aType, bType, aIsOriginal)
      } else {
        GQLAssert.raise(`Unknown type for object ${aType.name}: ${aType.constructor.name}`)
      }
    }
    return errors
  }

  private static scalarTypeEquals(typeA : any, typeB: any, aIsOriginal : boolean) {  
    if(typeA.name !== typeB.name) {
      if(aIsOriginal) {
        console.error(`Scalar type ${typeB.name} is missing..`)
      } else {
        console.error(`Scalar type ${typeA.name} is extra.`)
      }
      return 1
    }
    return 0
  }

  private static allEnumFieldsFromAInB(typeA : any, typeB: any, aIsOriginal : boolean) {  

    const aVals = typeA.getValues()
    const bVals = typeB.getValues()

    if(aVals.length === 0 || bVals.length === 0) {
      console.error(`There should never be empty enums: ${typeA.name}`)
      return 1
    }
    
    let errors = 0

    for(const aValue of aVals) {
      const bValue = typeB.getValue(aValue.name)
      if(bValue === undefined) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`-- Type ${typeB.name} is missing value ${aValue.name}.`)
        } else {
          console.error(`-- Type ${typeA.name} has extra value ${aValue.name}.`)
        }
      }
    }

    return errors
  }

  private static allFieldsFromAInB(typeA : any, typeB: any, aIsOriginal : boolean) {

    const aFields = typeA.getFields()
    const bFields = typeB.getFields()

    if(aFields.length === 0 || bFields.length === 0) {
      console.error(`There should never be empty types: ${typeA.name}`)
      return 1
    }

    let errors = 0

    for(const fieldName of Object.keys(aFields)) {
      const bField = bFields[fieldName]
      if(bField === undefined) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`-- Type ${typeB.name} is missing field ${fieldName}.`)
        } else {
          console.error(`-- Type ${typeA.name} has extra field ${fieldName}.`)
        }
        continue // Deeper check has no meaning. 
      }
      
      const fieldA = aFields[fieldName]
      const fieldB = bFields[fieldName]

      if(fieldA.type.toString() !== fieldB.type.toString()) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`---- Field ${typeB.name}.${fieldName} has incorrect type: ${fieldA.type} expected, ${fieldB.type} given.`)
        } else {
          console.error(`---- Field ${typeA.name}.${fieldName} has incorrect type: ${fieldB.type} expected, ${fieldA.type} given.`)
        }
      }

      errors = errors + AstTools.allArgsFromAInB(fieldA, fieldB, typeA, aIsOriginal)
    }
    return errors
  }


  private static allArgsFromAInB(fieldA : any, fieldB: any, type: any, aIsOriginal : boolean) {
    if(fieldA.args === null || fieldA.args === undefined) {
      return 0
    }

    let errors = 0

    if(fieldB.args === null || fieldB.args === undefined) {
      errors = errors + 1
      if(aIsOriginal) {
        console.error(`------ Field ${type.name}.${fieldB.name} does not have any arguments.`)
      } else {
        console.error(`------ Field ${type.name}.${fieldA.name} should not have any arguments.`)
      }
    }

    const argsA = Object.keys(fieldA.args)
    const argsB = Object.keys(fieldB.args)

    if(argsA.length !== argsB.length) {

      if(aIsOriginal) {
        console.error(`------ Field ${type.name}.${fieldB.name} has missmatching argument counts: ${argsA.length} expected, ${argsB.length} found.`)
      } else {
        console.error(`------ Field ${type.name}.${fieldB.name} has missmatching argument counts: ${argsB.length} expected, ${argsA.length} found.`)
      }
      return errors  // Deeper check is not useful. 
    }

    for(let i = 0; i < argsA.length; i++) {

      if(argsA[i] !== argsB[i]) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`-------- Field ${type.name}.${fieldB.name}, argument ${argsB[i]} has incorrect name: ${argsA[i]} expected.`)
        } else {
          console.error(`-------- Field ${type.name}.${fieldA.name}, argument ${argsA[i]} has incorrect na,e: ${argsA[i]} expected.`)
        }
      }

      const argA = fieldA.args[argsA[i]]
      const argB = fieldB.args[argsA[i]]

      if(String(argA.type) !== String(argB.type)) {
        errors = errors + 1
        if(aIsOriginal) {
          console.error(`-------- Field ${type.name}.${fieldB.name}, argument ${argsB[i]} has incorrect type: ${argA.type} expected, ${argB.type} given.`)
        } else {
          console.error(`-------- Field ${type.name}.${fieldA.name}, argument ${argsA[i]} has incorrect type: ${argB.type} expected, ${argA.type} given.`)
        }
      }
    }
    return errors
  }
}