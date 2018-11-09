import DefaultGenerator from '../default'
import ModelWhereInputGenerator from './query/modelWhereInputGenerator'

export default class MongoDbGenerator extends DefaultGenerator {
  modelWhereInput = new ModelWhereInputGenerator(this.typeRegistry, this)
}