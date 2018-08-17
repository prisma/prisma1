import { TypescriptGenerator, RenderOptions } from './TypescriptGenerator'

export class TypescriptDefinitionGenerator extends TypescriptGenerator {
  constructor(options) {
    super(options)
  }
  renderImports() {
    return `\
import { GraphQLResolveInfo, GraphQLSchema } from 'graphql'
import { IResolvers } from 'graphql-tools/dist/Interfaces'
import { BasePrismaOptions, Options } from 'prisma-lib'`
  }
  renderExports(options?: RenderOptions) {
    return super.renderExports(options)
  }
  renderDefinition(options?: RenderOptions) {
    return this.compile`\
${this.renderImports()}

export interface Query ${this.renderQueries()}

export interface Mutation ${this.renderMutations()}

export interface Subscription ${this.renderSubscriptions()}

export interface Exists ${this.renderExists()}

export interface Node {}

export interface Prisma {
  query: Query
  mutation: Mutation
  subscription: Subscription
  exists: Exists
  request: <T = any>(query: string, variables?: {[key: string]: any}) => Promise<T>
  delegate: Delegate;
delegateSubscription(fieldName: string, args?: {
    [key: string]: any;
}, infoOrQuery?: GraphQLResolveInfo | string, options?: Options): Promise<AsyncIterator<any>>;
getAbstractResolvers(filterSchema?: GraphQLSchema | string): IResolvers;
}

export interface Delegate {
  (
    operation: 'query' | 'mutation',
    fieldName: string,
    args: {
      [key: string]: any
    },
    infoOrQuery?: GraphQLResolveInfo,
    options?: Options,
  ): Promise<any>
  query: DelegateQuery
  mutation: DelegateMutation
  subscription: Subscription
}

export interface DelegateQuery ${this.renderDelegateQueries()}

export interface DelegateMutation ${this.renderDelegateMutations()}

export interface BindingConstructor<T> {
  new(options?: BasePrismaOptions): T
}
/**
 * Type Defs
*/

${this.renderTypedefs()}

${this.renderExports(options)}

/**
 * Types
*/

${this.renderTypes()}`
  }
  formatDefinition(text) {
    const lines = text.split('\n')
    return lines
      .map(l =>
        l
          .replace('export const', 'export declare const')
          .replace('export type', 'export declare type'),
      )
      .join('\n')
  }
  renderJavascript(options?: RenderOptions) {
    const args = this.renderPrismaClassArgs(options)
    return `\
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var prisma_lib_1 = require("prisma-lib");
/**
 * Type Defs
 */

${this.renderTypedefs()}

exports.Prisma = prisma_lib_1.makePrismaBindingClass(${args});
exports.prisma = new exports.Prisma();
`
  }
}
