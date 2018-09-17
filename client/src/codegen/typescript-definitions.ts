import { TypescriptGenerator } from './typescript-client'

export interface RenderOptions {
  endpoint?: string
  secret?: string
}

export class TypescriptDefinitionsGenerator extends TypescriptGenerator {
  renderExports(options?: RenderOptions) {
    return ``
  }
}
