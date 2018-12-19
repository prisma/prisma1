import { TypescriptGenerator, RenderOptions } from './typescript-client'

export class TypescriptDefinitionsGenerator extends TypescriptGenerator {
  renderExports(options?: RenderOptions) {
    return `export const prisma: Prisma;`
  }
}
