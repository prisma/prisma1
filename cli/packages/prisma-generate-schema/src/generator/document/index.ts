import DefaultGenerator from '../default'
import ModelWhereInputGenerator from './query/modelWhereInputGenerator'
import ModelOrderByInputGenerator from './query/modelOrderByInputGenerator'
import ModelSubscriptionWhereInputGenerator from './subscription/modelSubscriptionWhereInputGenerator'

export default class MongoDbGenerator extends DefaultGenerator {
  modelWhereInput = new ModelWhereInputGenerator(this.typeRegistry, this)
  modelOrderByInput = new ModelOrderByInputGenerator(this.typeRegistry, this)
  modelSubscriptionWhereInput = new ModelSubscriptionWhereInputGenerator(this.typeRegistry, this)
}