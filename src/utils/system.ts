export function exitWithPrintingNewLine(code?: number) {
  process.stdout.write('\n')
  process.exit(code)
}

export function sleep(milliSeconds: number): Promise<void> {
  return new Promise<void>(resolve => setTimeout(resolve, milliSeconds))
}