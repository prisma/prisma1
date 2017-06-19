import TestResolver from '../../src/system/TestResolver'
import { TestSystemEnvironment } from '../../src/types'
import TestOut from '../../src/system/TestOut'
import { Config } from '../../src/utils/config'

export function testEnvironment(storage: any): TestSystemEnvironment {
  const resolver = new TestResolver(storage)
  const config = new Config(resolver)

  return {
    resolver: resolver,
    out: new TestOut(),
    config: config
  }
}
