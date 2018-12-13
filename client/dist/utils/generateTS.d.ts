import { TypescriptGenerator } from '../codegen/typescript-client';
export declare class TestTypescriptGenerator extends TypescriptGenerator {
    renderImports(): string;
}
export declare function generateTypescript(schemaString: string): Promise<void>;
