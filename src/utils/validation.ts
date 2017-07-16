export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z][a-zA-Z0-9]*$/.test(projectName)
}
