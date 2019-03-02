import RelationalSubscriptionGenerator from '../../default/subscription/subscriptionGenerator'
import { IGQLType } from 'prisma-datamodel'

export default class SubscriptionGenerator extends RelationalSubscriptionGenerator {
  protected shouldGenerateSubscription(type: IGQLType) {
    return !type.isEmbedded && super.shouldGenerateSubscription(type)
  }
}
