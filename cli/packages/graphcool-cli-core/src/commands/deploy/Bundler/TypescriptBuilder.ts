import * as ts from 'typescript'
// import * as globby from 'globby'
import * as path from 'path'
const debug = require('debug')('ts-builder')

export default class TypescriptBuilder {
  buildDir: string
  definitionDir: string
  constructor(definitionDir: string, buildDir: string) {
    this.definitionDir = definitionDir
    this.buildDir = buildDir
  }
  //
  // async getFileNames() {
  //   return globby(['**/*.js', '**/*.ts', '!node_modules', '!**/node_modules'], {cwd: this.buildDir})
  // }

  async compile(fileNames: string[]) {
    // const fileNames = await this.getFileNames()
    debug('starting compile', fileNames)
    const program = ts.createProgram(fileNames, this.config)
    debug('created program')

    const emitResult = program.emit()
    debug('emitted')

    const allDiagnostics = ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics)

    allDiagnostics.forEach(diagnostic => {
      if (!diagnostic.file) {
        console.log(diagnostic)
      }
      const {line, character} = diagnostic.file!.getLineAndCharacterOfPosition(diagnostic.start!)
      const message = ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n')
      console.log(`${diagnostic.file!.fileName} (${line + 1},${character + 1}): ${message}`)
    })

    if (emitResult.emitSkipped) {
      throw new Error('Typescript compilation failed')
    }

    return emitResult.emittedFiles
  }

  get config(): ts.CompilerOptions {
    return {
      ...baseCompilerOptions,
      lib: ['lib.es2017.d.ts'],
      rootDir: this.definitionDir,
      outDir: this.buildDir,
      typeRoots: [
        path.join('this-folder', 'does-not-exist'),
        path.join(__dirname, '../../../../node_modules/@types'),
        path.join(__dirname, '../../../../../../node_modules/@types'),
        path.join(this.definitionDir,  'typings'),
        path.join(this.definitionDir,  'node_modules/@types'),
      ]
    }
  }
}

export const baseCompilerOptions = {
  preserveConstEnums: true,
  strictNullChecks: true,
  sourceMap: false,
  target: ts.ScriptTarget.ES5,
  moduleResolution: ts.ModuleResolutionKind.NodeJs,
  lib: ['lib.es2017.d.ts'],
  allowJs: true,
  listEmittedFiles: true,
  skipLibCheck: true,
  allowSyntheticDefaultImports: true
}