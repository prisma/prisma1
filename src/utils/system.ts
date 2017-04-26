export function exitWithPrintingNewLine(code?: number) {
  process.stdout.write('\n')
  process.exit(code)
}
