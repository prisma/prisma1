import { IGQLField } from '../datamodel/model'

export default abstract class GQLAssert {
  public static raise(message: string) {
    throw new Error(message)
  }
  public static raiseIf(condition: boolean, message: string) {
    if (condition) {
      GQLAssert.raise(message)
    }
  }
}
