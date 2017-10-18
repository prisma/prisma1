import {
  MigrationActionType,
  MigrationErrorMessage,
  MigrationMessage,
} from '../types/common'
import figures = require('figures')
import chalk from 'chalk'
import { Output } from './index'
import { makePartsEnclodesByCharacterBold } from './util'
import * as groupBy from 'lodash.groupby'

export class MigrationPrinter {
  out: Output
  constructor(out: Output) {
    this.out = out
  }
  printMessages(migrationMessages: MigrationMessage[]) {
    // group types,
    const groupedByType = groupBy(migrationMessages, m => m.type)
    Object.keys(groupedByType).forEach(type => {
      const typeMessages = groupedByType[type]
      this.out.log('\n' + printType(type) + '\n')
      const groupedByName = groupBy(typeMessages, m => m.name.split('.')[0])
      Object.keys(groupedByName).forEach(name => {
        this.out.log(`  ${chalk.bold(name)}`)
        const nameMessages = groupedByName[name]
        nameMessages.forEach(this.printMigrationMessage, this)
      })
    })
    this.out.log('')
  }
  printErrors(errors: MigrationErrorMessage[]) {
    const groupedByType = groupBy(errors, e => e.type)
    Object.keys(groupedByType).forEach(type => {
      const typeErrors = groupedByType[type]
      this.out.log('\n  ' + chalk.bold(type))
      typeErrors.forEach(error => {
        const outputMessage = makePartsEnclodesByCharacterBold(
          error.description,
          `\``,
        )
        this.out.log(`    ${chalk.red(figures.cross)} ${outputMessage}`)
      })
    })
  }

  private printMigrationMessage(
    migrationMessage: MigrationMessage,
    index: number,
  ) {
    const actionType = this.getType(migrationMessage.description)
    const symbol = this.getSymbol(actionType)
    const description = makePartsEnclodesByCharacterBold(
      migrationMessage.description,
      '`',
    )
    const outputMessage = `   ${symbol} ${description}`
    this.out.log(outputMessage)

    migrationMessage.subDescriptions!.forEach((subMessage, index2) => {
      const actionType2 = this.getType(subMessage.description)
      const symbol2 = this.getSymbol(actionType2)
      const outputMessage2 = makePartsEnclodesByCharacterBold(
        subMessage.description,
        '`',
      )
      const lastLine = index2 === migrationMessage.subDescriptions!.length - 1
      const endSymbol = lastLine ? '└' : '├'
      this.out.log(`   ${endSymbol}─ ${symbol2}  ${outputMessage2}`)
    })
  }

  private getType(message: string): MigrationActionType {
    if (message.indexOf('create') >= 0) {
      return 'create'
    } else if (message.indexOf('update') >= 0) {
      return 'update'
    } else if (
      message.indexOf('delete') >= 0 ||
      message.indexOf('remove') >= 0
    ) {
      return 'delete'
    }
    return 'unknown'
  }

  private getSymbol(type: MigrationActionType): string {
    switch (type) {
      case 'create':
        return chalk.green('+')
      case 'delete':
        return chalk.red('-')
      case 'update':
        return chalk.blue('*')
      case 'unknown':
        return chalk.cyan('?')
    }
  }
}

const spaces = (n: number) => Array(n + 1).join(' ')

const printType = (type: string) =>
  chalk.bold.underline(type.replace(/\b\w/g, l => l.toUpperCase()) + 's')
