import { TypescriptGenerator } from './typescript-client'
import { getExistsFlowTypes } from '../../utils'

import * as prettier from 'prettier'
import { codeComment } from '../../utils/codeComment'

import * as os from 'os'

export interface RenderOptions {
  endpoint?: string
  secret?: string
}

export class FlowGenerator extends TypescriptGenerator {
  genericsDelimiter = ':'
  lineBreakDelimiter = ','
  partialType = '$Shape'
  exportPrisma = true

  prismaInterface = 'PrismaInterface'

  typeObjectType = 'type'

  renderImports() {
    return `\
/**
 * @flow
 */

${codeComment}

import type { DocumentNode } from 'graphql'
import type { BasePrismaOptions as BPOType } from 'prisma-client-lib'
import { makePrismaClientClass, Model } from 'prisma-client-lib'
import { typeDefs } from './prisma-schema'

type NodePromise = Promise<Node>`
  }
  renderClientConstructor() {
    return `export type ClientConstructor<T> = (options?: BPOType) => T
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
    return `export type AtLeastOne<T> = $Shape<T>`
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

    return `export const Prisma: ClientConstructor<${
      this.prismaInterface
    }> = makePrismaClientClass(${args})

export const prisma = new Prisma()`
  }
  renderTypedefsFirstLine() {
    return `// @flow${os.EOL}`
  }
  static replaceEnv(str: string): string {
    const regex = /\${env:(.*?)}/
    const match = regex.exec(str)
    // tslint:disable-next-line:prefer-conditional-expression
    if (match) {
      return FlowGenerator.replaceEnv(
        `${str.slice(0, match.index)}$\{process.env['${
          match[1]
        }'] || ""}}${str.slice(match[0].length + match.index)}`,
      )
    } else {
      return `\`${str}\``
    }
  }
}
