import { INormalizer } from './normalizer'
import { ISDL } from 'prisma-datamodel'

export default class NormalizerGroup implements INormalizer {
  private baseNormalizers: INormalizer[]

  constructor(baseNormalizers: INormalizer[]) {
    this.baseNormalizers = baseNormalizers
  }

  public normalize(model: ISDL) {
    for (const normalizer of this.baseNormalizers) {
      normalizer.normalize(model)
    }
  }
}
