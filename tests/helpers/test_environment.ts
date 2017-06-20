import TestResolver from './test_resolver'
import {Resolver} from '../../src/types'
import TestOut from './test_out'
import { Config } from '../../src/utils/config'

export interface TestSystemEnvironment {
  out: TestOut
  resolver: Resolver
  config: Config
}

export function testEnvironment(storage: any): TestSystemEnvironment {
  const resolver = new TestResolver(storage)
  const config = new Config(resolver)

  return {
    resolver: resolver,
    out: new TestOut(),
    config: config
  }
}
