import { TypescriptGenerator } from './typescript-client';
import * as prettier from 'prettier';
export interface RenderOptions {
    endpoint?: string;
    secret?: string;
}
export declare class FlowGenerator extends TypescriptGenerator {
    genericsDelimiter: string;
    lineBreakDelimiter: string;
    partialType: string;
    exportPrisma: boolean;
    prismaInterface: string;
    renderImports(): string;
    renderClientConstructor(): string;
    format(code: string, options?: prettier.Options): string;
    renderAtLeastOne(): string;
    renderGraphQL(): string;
    renderInputListType(type: any): string;
    renderExists(): string;
    renderExports(options?: RenderOptions): string;
}
