import { Client, Output } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import { repeat } from 'lodash'

export type FileType = 'nodes' | 'relations' | 'lists'

export interface ExportRequest {
  fileType: FileType
  cursor: ExportCursor
}

export interface ExportCursor {
  table: number
  row: number
  field: number
  array: number
}

export class Exporter {
  client: Client
  exportPath: string
  out: Output
  constructor(exportPath: string, client: Client, out: Output) {
    this.client = client
    this.exportPath = exportPath
    this.out = out
  }

  async download(projectId: string) {
    this.makeDirs()
    await this.downloadFiles('nodes', projectId)
    await this.downloadFiles('lists', projectId)
    await this.downloadFiles('relations', projectId)
  }

  makeDirs() {
    fs.mkdirpSync(path.join(this.exportPath, 'nodes/'))
    fs.mkdirpSync(path.join(this.exportPath, 'lists/'))
    fs.mkdirpSync(path.join(this.exportPath, 'relations/'))
  }

  async downloadFiles(fileType: FileType, projectId: string) {
    const before = Date.now()
    this.out.action.start(`Downloading ${fileType}`)

    let cursor: ExportCursor = {
      table: 0,
      row: 0,
      field: 0,
      array: 0,
    }

    const cursorSum = c =>
      Object.keys(c).reduce((acc, curr) => acc + c[curr], 0)

    const leadingZero = (n: number, zeroes: number = 6) =>
      repeat('0', Math.max(zeroes - String(n).length, 0)) + n

    let count = 1
    const filesDir = path.join(this.exportPath, `${fileType}/`)
    while (cursorSum(cursor) >= 0) {
      const data = await this.client.download(
        projectId,
        JSON.stringify({
          fileType,
          cursor,
        }),
      )

      const jsonString = JSON.stringify({
        valueType: fileType,
        values: data.out.jsonElements,
      })

      fs.writeFileSync(
        path.join(filesDir, `${leadingZero(count)}.json`),
        jsonString,
      )

      cursor = data.cursor
      count++
    }

    this.out.action.stop(chalk.cyan(`${Date.now() - before}ms`))
  }
}
