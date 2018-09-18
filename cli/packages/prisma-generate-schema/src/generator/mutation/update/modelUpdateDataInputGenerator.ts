import { IGQLType } from '../../../datamodel/model'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';


export default class ModelUpdateDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateDataInput`
  }
}