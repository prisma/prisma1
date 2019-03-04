import chalk from 'chalk'

export function makePartsEnclodesByCharacterBold(
  str: string,
  character: string,
): string {
  const components = str.split(character)
  for (let i = 0; i < components.length; i++) {
    if (i % 2 === 1) {
      components[i] = chalk.bold(components[i])
    }
  }
  return components.join(chalk.bold(`\``))
}

export function regionEnumToOption(regionEnum: string): string {
  return regionEnum.toLowerCase().replace(/_/g, '-')
}
