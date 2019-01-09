import DefaultGenerator from '../default'
import ModelWhereInputGenerator from './query/modelWhereInputGenerator'
import ModelOrderByInputGenerator from './query/modelOrderByInputGenerator'

export default class MongoDbGenerator extends DefaultGenerator {
  modelWhereInput = new ModelWhereInputGenerator(this.typeRegistry, this)
  modelOrderByInput = new ModelOrderByInputGenerator(this.typeRegistry, this)
}