import { TypescriptGenerator, RenderOptions } from './typescript-client'

export class JavascriptGenerator extends TypescriptGenerator {
  constructor(options) {
    super(options)
  }
  renderJavascript(options?: RenderOptions) {
    const args = this.renderPrismaClassArgs(options)
    return `\
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var prisma_lib_1 = require("prisma-lib");
var typeDefs = require("./graphql").typeDefs

exports.Prisma = prisma_lib_1.makePrismaBindingClass(${args});
exports.prisma = new exports.Prisma();
`
  }
}