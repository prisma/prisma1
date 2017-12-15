import * as path from 'path'
import * as fs from 'fs-extra'
import { Client, Output } from 'graphcool-cli-engine'
import * as globby from 'globby'
import { Validator } from './Validator'
import chalk from 'chalk'

export interface Files {
  lists: string[]
  nodes: string[]
  relations: string[]
}

export interface State {
  lists: number
  nodes: number
  relations: number
}

const defaultState = {
  nodes: 0,
  lists: 0,
  relations: 0,
}

export class Importer {
  importPath: string
  client: Client
  types: string
  out: Output
  statePath: string
  constructor(importPath: string, types: string, client: Client, out: Output) {
    if (!fs.pathExistsSync(importPath)) {
      throw new Error(`Import path ${importPath} does not exist`)
    }
    this.importPath = importPath
    this.client = client
    this.types = types
    this.out = out
    this.statePath = path.join(this.importPath, 'state.json')
  }
  saveState(state) {
    fs.writeFileSync(this.statePath, JSON.stringify(state))
  }
  getState(): State {
    try {
      const json = fs.readJsonSync(this.statePath)
      return {
        ...defaultState,
        ...json,
      }
    } catch (e) {
      //
    }

    return defaultState
  }
  getNumber(fileName: string) {
    const fileRegex = /.*?(\d+)\.json/
    const match = fileName.match(fileRegex)
    if (match) {
      return parseInt(match[1], 10)
    }

    return 0
  }
  async upload(projectId: string) {
    let before = Date.now()
    this.out.action.start('Validating data')
    const files = this.getFiles()
    this.validateFiles(files)
    this.out.action.stop(chalk.cyan(`${Date.now() - before}ms`))
    before = Date.now()
    this.out.log('\nUploading nodes...')
    const state = this.getState()

    for (const fileName of files.nodes) {
      const n = this.getNumber(fileName)
      if (state.nodes >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(projectId, file)
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }

      state.nodes = n
      this.saveState(state)
    }
    this.out.log(
      'Uploading nodes done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
    before = Date.now()
    this.out.log('\nUploading lists')
    for (const fileName of files.lists) {
      const n = this.getNumber(fileName)
      if (state.lists >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(projectId, file)
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }
      state.lists = n
      this.saveState(state)
    }
    this.out.log(
      'Uploading lists done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
    before = Date.now()
    this.out.log('\nUploading relations')
    for (const fileName of files.relations) {
      const n = this.getNumber(fileName)
      if (state.relations >= n) {
        this.out.log(`Skipping file ${fileName} (already imported)`)
        continue
      }
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      const result = await this.client.upload(projectId, file)
      if (result.length > 0) {
        this.out.log(this.out.getStyledJSON(result))
        this.out.exit(1)
      }
      state.relations = n
      this.saveState(state)
    }
    this.saveState(defaultState)
    this.out.log(
      'Uploading relations done ' + chalk.cyan(`${Date.now() - before}ms`),
    )
  }

  validateFiles(files: Files) {
    const validator = new Validator(this.types)
    for (const fileName of files.nodes) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
    for (const fileName of files.lists) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
    for (const fileName of files.relations) {
      const file = fs.readFileSync(fileName, 'utf-8')
      const json = JSON.parse(file)
      validator.validateImportData(json)
    }
  }

  getFiles(): Files {
    return {
      lists: globby
        .sync('*.json', {
          cwd: path.join(this.importPath, 'lists/'),
        })
        .map(p => path.join(this.importPath, 'lists/', p)),
      nodes: globby
        .sync('*.json', {
          cwd: path.join(this.importPath, 'nodes/'),
        })
        .map(p => path.join(this.importPath, 'nodes/', p)),
      relations: globby
        .sync('*.json', {
          cwd: path.join(this.importPath, 'relations/'),
        })
        .map(p => path.join(this.importPath, 'relations/', p)),
    }
  }
}
