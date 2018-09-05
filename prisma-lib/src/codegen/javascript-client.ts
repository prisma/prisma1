import { TypescriptGenerator, RenderOptions } from './TypescriptGenerator'

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
/**
 * Type Defs
 */

${this.renderTypedefs()}

exports.Prisma = prisma_lib_1.makePrismaBindingClass(${args});
exports.prisma = new exports.Prisma();
`
  }
}