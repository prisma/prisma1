import {Command} from '../types'
import {
  usagePush,
  usagePull,
  usageProjects,
  usageInit,
  usageAuth,
  usageVersion,
  usageConsole,
  usageExport,
  usageEndpoints,
  usagePlayground,
  usageStatus,
  usageQuickstart,
  usageDelete
} from './usage'

export function optionsForCommand(command: Command): string[] {
  switch (command) {
    case 'init' || 'create':
      return ['name', 'n', 'alias', 'a', 'region', 'r', 'schema', 's', 'copy', 'c', 'output', 'o', 'copy-options']
    case 'push':
      return ['force', 'f']
    case 'delete':
      return ['project', 'p']
    case 'pull':
      return ['project', 'p', 'output', 'o', 'force', 'f']
    case 'auth':
      return ['token', 't']
  }
  return []
}

export function usageForCommand(command: Command): string {
  switch (command) {
    case 'pull': return usagePull
    case 'push': return usagePush
    case 'projects': return usageProjects
    case 'init': return usageInit
    case 'auth': return usageAuth
    case 'version': return usageVersion
    case 'console': return usageConsole
    case 'export': return usageExport
    case 'endpoints': return usageEndpoints
    case 'playground': return usagePlayground
    case 'status': return usageStatus
    case 'quickstart': return usageQuickstart
    case 'delete': return usageDelete
  }
  return ''
}