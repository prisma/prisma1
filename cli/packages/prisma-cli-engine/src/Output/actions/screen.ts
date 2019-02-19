function termWidth(stream: any): number {
  if (!stream.isTTY) {
    return 80
  }
  const width = stream.getWindowSize()[0]
  if (width < 1) {
    return 80
  }
  if (width < 40) {
    return 40
  }
  return width
}

export const stdtermwidth = (global as any).columns || termWidth(process.stdout)
export const errtermwidth = (global as any).columns || termWidth(process.stderr)
