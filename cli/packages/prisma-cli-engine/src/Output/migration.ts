import { MigrationActionType, MigrationMessage } from '../types/common'
import figures = require('figures')
import chalk from 'chalk'
import { Output } from './index'
import { makePartsEnclodesByCharacterBold } from './util'
import * as groupBy from 'lodash.groupby'
import { SchemaError, MigrationStep } from '../Client/types'

const b = s => `\`${chalk.bold(s)}\``

export class MigrationPrinter {
  out: Output
  constructor(out: Output) {
    this.out = out
  }
  printMessages(steps: MigrationStep[]) {
    this.printTypes(steps)
    this.printEnums(steps)
    this.printRelations(steps)
  }
  printTypes(allSteps: MigrationStep[]) {
    const steps = allSteps.filter(
      s => s.model || (s.type === 'CreateModel' || s.type === 'DeleteModel'),
    )
    const groupedByModel = groupBy(steps, s => s.model || s.name)
    Object.keys(groupedByModel).forEach(model => {
      this.out.log(`\n  ${chalk.bold(model)} (Type)`)
      const modelSteps = groupedByModel[model]
      modelSteps.forEach(this.printStep)
    })
  }

  printStep = (step: MigrationStep) => {
    const pad = '  '
    switch (step.type) {
      case 'CreateModel': {
        this.out.log(
          `${pad}${this.getSymbol('create')} Created type ${b(step.name)}`,
        )
        break
      }
      case 'DeleteModel': {
        this.out.log(
          `${pad}${this.getSymbol('delete')} Deleted type ${b(step.name)}`,
        )
        break
      }
      case 'CreateField': {
        const typeString = this.printType(
          step.cf_typeName!,
          step.cf_isRequired!,
          step.cf_isList!,
        )
        this.out.log(
          `${pad}${this.getSymbol('create')} Created field ${b(
            step.name,
          )} of type ${b(typeString)}`,
        )
        break
      }
      case 'DeleteField': {
        this.out.log(
          `${pad}${this.getSymbol('delete')} Deleted field ${b(step.name)}`,
        )
        break
      }
      case 'UpdateField': {
        const typeString = this.printType(
          step.cf_typeName!,
          step.cf_isRequired!,
          step.cf_isList!,
        )
        this.out.log(
          `${pad}${this.getSymbol('update')} Updated field ${b(
            step.name,
          )}${this.getUpdateFieldActions(step)}`,
        )
        break
      }
    }
  }

  getUpdateFieldActions(step: MigrationStep) {
    const messages: string[] = []
    if (step.isRequired) {
      messages.push(`became required`)
    }

    if (step.isRequired === false) {
      messages.push(`is not required anymore`)
    }

    if (step.isList) {
      messages.push(`became a list`)
    }

    if (step.isList === false) {
      messages.push(`is not a list anymore`)
    }

    if (step.isUnique) {
      messages.push(`became unique`)
    }

    if (step.enum) {
      messages.push(`now uses enum ${step.enum}`)
    }

    if (step.defaultValue) {
      messages.push(`got default value ${step.defaultValue}`)
    }

    if (messages.length === 0) {
      return ''
    }

    return '. It ' + messages.join(', ') + '.'
  }

  printType(typeName: string, isRequired: boolean, isList?: boolean) {
    if (isList) {
      return `[${typeName}!]!`
    }

    return `${typeName}${isRequired ? '!' : ''}`
  }

  printEnums(steps: MigrationStep[]) {
    const pad = '  '
    steps.forEach(step => {
      switch (step.type) {
        case 'CreateEnum':
          this.printEnumName(step)
          this.out.log(
            `${pad}${this.getSymbol('create')} Created enum ${
              step.name
            } with values ${step.ce_values &&
              step.ce_values.map(b).join(', ')}`,
          )
          break
        case 'UpdateEnum':
          this.printEnumName(step)
          this.out.log(
            `${pad}${this.getSymbol('update')} Updated enum ${step.name}`,
          )
          break
        case 'DeleteEnum':
          this.printEnumName(step)

          this.out.log(
            `${pad}${this.getSymbol('delete')} Deleted enum ${step.name}`,
          )
          break
      }
    })
  }

  printRelations(steps: MigrationStep[]) {
    const pad = '  '
    steps.forEach(step => {
      switch (step.type) {
        case 'CreateRelation':
          this.printRelationName(step)
          this.out.log(
            `${pad}${this.getSymbol('create')} Created relation between ${
              step.leftModel
            } and ${step.rightModel}`,
          )
          break
        case 'DeleteRelation':
          this.printRelationName(step)
          this.out.log(
            `${pad}${this.getSymbol('delete')} Deleted relation between ${
              step.leftModel
            } and ${step.rightModel}`,
          )
          break
      }
    })
  }

  printRelationName(step: MigrationStep) {
    this.out.log(`\n  ${chalk.bold(step.name)} (Relation)`)
  }

  printEnumName(step: MigrationStep) {
    this.out.log(`\n  ${chalk.bold(step.name)} (Enum)`)
  }

  printErrors(errors: SchemaError[]) {
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

  printWarnings(warnings: SchemaError[]) {
    const groupedByType = groupBy(warnings, e => e.type)
    Object.keys(groupedByType).forEach(type => {
      const typeErrors = groupedByType[type]
      this.out.log('\n  ' + chalk.bold(type))
      typeErrors.forEach(error => {
        const outputMessage = makePartsEnclodesByCharacterBold(
          error.description,
          `\``,
        )
        this.out.log(`    ${chalk.yellow.bold('!')} ${outputMessage}`)
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
        return chalk.yellow('~')
      case 'unknown':
        return chalk.cyan('?')
    }
  }
}

const spaces = (n: number) => Array(n + 1).join(' ')

const printType = (type: string) =>
  chalk.bold.underline(type.replace(/\b\w/g, l => l.toUpperCase()) + 's')
