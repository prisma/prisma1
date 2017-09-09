import { MigrationActionType, MigrationErrorMessage, MigrationMessage } from '../types'
import figures = require('figures')
import * as chalk from 'chalk'
import { Output } from './index'
import { makePartsEnclodesByCharacterBold } from './util'

export class MigrationPrinter {
  out: Output
  constructor(out: Output) {
    this.out = out
  }
  printMessages(migrationMessages: MigrationMessage[]) {
    migrationMessages.forEach((migrationMessage, index) => {
      const actionType = this.getMigrationActionType(migrationMessage.description)
      const symbol = this.getSymbolForMigrationActionType(actionType)
      const description = makePartsEnclodesByCharacterBold(migrationMessage.description, `\``)
      const outputMessage = `${index > 0 ? `  |` : ``}\n  | (${symbol})  ${description}\n`
      this.out.log(outputMessage)
      migrationMessage.subDescriptions!.forEach((subMessage, index2) => {
        const actionType2 = this.getMigrationActionType(subMessage.description)
        const symbol2 = this.getSymbolForMigrationActionType(actionType2)
        const outputMessage2 = makePartsEnclodesByCharacterBold(subMessage.description, `\``)
        const lastLine = index2 === migrationMessage.subDescriptions!.length - 1
        const endSymbol = lastLine ? '└' : '├'
        this.out.log(`  ${endSymbol}── (${symbol2})  ${outputMessage2}\n`)
      })
    })
  }

  printErrors(errors: MigrationErrorMessage[]) {
    const indentation = spaces(2)
    errors.forEach(error => {
      const outputMessage = makePartsEnclodesByCharacterBold(error.description, `\``)
      this.out.log(`${indentation}${chalk.red(figures.cross)} ${outputMessage}\n`)
    })
  }

  private getMigrationActionType(message: string): MigrationActionType {
    if (message.indexOf('create') >= 0) {
      return 'create'
    } else if (message.indexOf('update') >= 0) {
      return 'update'
    } else if (message.indexOf('delete') >= 0 || message.indexOf('remove') >= 0) {
      return 'delete'
    }
    return 'unknown'
  }

  private getSymbolForMigrationActionType(type: MigrationActionType): string {
    switch (type) {
      case 'create': return '+'
      case 'delete': return '-'
      case 'update': return '*'
      case 'unknown': return '?'
    }
  }

}

const spaces = (n: number) => Array(n + 1).join(' ')

