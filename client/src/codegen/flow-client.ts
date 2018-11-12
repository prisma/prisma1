import { TypescriptGenerator } from './typescript-client'
import { getExistsFlowTypes } from '../utils'

import * as prettier from 'prettier'
import { codeComment } from '../utils/codeComment';

export interface RenderOptions {
  endpoint?: string
  secret?: string
}

export class FlowGenerator extends TypescriptGenerator {
  genericsDelimiter = ':'
  lineBreakDelimiter = ','
  partialType = '$Shape'
  exportPrisma = false

  prismaInterface = 'PrismaInterface'
  renderImports() {
    return `\
/**
 * @flow
 */

${codeComment}

import type { GraphQLSchema, DocumentNode } from 'graphql'
import type { BasePrismaOptions as BPOType, Options } from 'prisma-client-lib'
import { makePrismaClientClass } from 'prisma-client-lib'
import { typeDefs } from './prisma-schema'

type NodePromise = Promise<Node>`
  }
  renderClientConstructor() {
    return `export type ClientConstructor<T> = (options?: BPOType) => T
}
`
  }
  format(code: string, options: prettier.Options = {}) {
    return prettier.format(code, {
      ...options,
      parser: 'flow',
    })
  }
  renderAtLeastOne() {
    // TODO: as soon as flow has a clean solution for at least one, implement it here
    return `type AtLeastOne<T> = $Shape<T>`
  }
  renderGraphQL() {
    return `$graphql: <T: mixed>(query: string, variables?: {[key: string]: mixed}) => Promise<T>;`
  }
  renderInputListType(type) {
    return `${type}[]`
  }
  renderExists() {
    const queryType = this.schema.getQueryType()
    if (queryType) {
      return `${getExistsFlowTypes(queryType)}`
    }
    return ''
  }
  renderExports(options?: RenderOptions) {
    const args = this.renderPrismaClassArgs(options)
    
    return `export const Prisma: ClientConstructor<PrismaInterface> = makePrismaClientClass(${args})

export const prisma = new Prisma()`
  }
}
