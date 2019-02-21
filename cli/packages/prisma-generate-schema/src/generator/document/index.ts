import DefaultGenerator from '../default'
import ModelWhereInputGenerator from './query/modelWhereInputGenerator'
import ModelOrderByInputGenerator from './query/modelOrderByInputGenerator'
import ModelSubscriptionWhereInput from './subscription/modelSubscriptionWhereInputGenerator'
import ModelRestrictedWhereInputGenerator from './query/modelRestrictedWhereInputGenerator'
import SubscriptionGenerator from './subscription/subscriptionGenerator'

export default class MongoDbGenerator extends DefaultGenerator {
  modelWhereInput = new ModelWhereInputGenerator(this.typeRegistry, this)
  modelOrderByInput = new ModelOrderByInputGenerator(this.typeRegistry, this)
  modelSubscriptionWhereInput = new ModelSubscriptionWhereInput(
    this.typeRegistry,
    this,
  )
  modelRestrictedWhereInput = new ModelRestrictedWhereInputGenerator(
    this.typeRegistry,
    this,
  )
  subscription = new SubscriptionGenerator(this.typeRegistry, this)
}
