export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z]{1,1}[a-zA-Z0-9]*$/.test(projectName)
}
