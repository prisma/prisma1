import RelationalModelSubscriptionWhereInput from '../../default/subscription/modelSubscriptionWhereInputGenerator'

export default class ModelSubscriptionWhereInput extends RelationalModelSubscriptionWhereInput {
  protected getSupportedLogicalOperators() {
    return  ['AND']
  }
}