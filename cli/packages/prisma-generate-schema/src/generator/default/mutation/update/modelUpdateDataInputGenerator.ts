import { IGQLType } from 'prisma-datamodel'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';


export default class ModelUpdateDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateDataInput`
  }
}