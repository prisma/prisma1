export const consoleURL = (token: string, projectName?: string) =>
  `https://console.graph.cool/token?token=${token}${projectName ? `&redirect=/${encodeURIComponent(projectName)}` : ''}`
export const playgroundURL = (token: string, projectName: string) =>
  `https://console.graph.cool/token?token=${token}&redirect=/${encodeURIComponent(projectName)}/playground`
