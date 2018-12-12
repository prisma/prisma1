import { TypescriptGenerator, RenderOptions } from './typescript-client';
import * as prettier from 'prettier';
export declare class JavascriptGenerator extends TypescriptGenerator {
    constructor(options: any);
    format(code: string, options?: prettier.Options): string;
    renderJavascript(options?: RenderOptions): string;
    renderModels(): string;
}
