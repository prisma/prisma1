import { clusterEndpointMapReverse } from '../constants'
import { URL } from 'url'

function getClusterName(origin): string {
  if (clusterEndpointMapReverse[origin]) {
    return clusterEndpointMapReverse[origin]
  }

  if (origin.endsWith('prisma.sh')) {
    return origin.split('_')[0].replace(/https?:\/\//, '')
  }

  if (isLocal(origin)) {
    return 'local'
  }

  return 'default'
}

const getWorkspaceFromPrivateOrigin = (origin: string) => {
  const split = origin.split('_')
  if (split.length > 1) {
    return split[1].split('.')[0]
  }

  return null
}

const isLocal = origin =>
  origin.includes('localhost') || origin.includes('127.0.0.1')

export interface ParseEndpointResult {
  service: string
  clusterBaseUrl: string
  stage: string
  isPrivate: boolean
  local: boolean
  shared: boolean
  workspaceSlug: string | null
  clusterName: string
}

export function parseEndpoint(endpoint: string): ParseEndpointResult {

  /*
    Terminology:
      local - hosted locally using docker and accessed using localhost or prisma or local web proxy like domain.dev
      shared - demo server
      isPrivate - private hosted by Prisma or private and self-hosted, important that in our terminology a local server is not private
  */

  const url = new URL(endpoint)
  const splittedPath = url.pathname.split('/')
  // assuming, that the pathname always starts with a leading /, we always can ignore the first element of the split array
  const service =
    splittedPath.length > 3 ? splittedPath[2] : splittedPath[1] || 'default'
  const stage =
    splittedPath.length > 3 ? splittedPath[3] : splittedPath[2] || 'default'
  
  // This logic might break for self-hosted servers incorrectly yielding a "workspace" simply if the UX has
  // enough "/"es like if https://custom.dev/not-a-workspace/ is the base Prisma URL then for default/default service/stage
  // pair. This function would incorrectly return not-a-workspace as a workspace. 
  let workspaceSlug = splittedPath.length > 3 ? splittedPath[1] : null

  const shared = ['eu1.prisma.sh', 'us1.prisma.sh'].includes(url.host)

  // When using localAliases, do an exact match because of 'prisma' option which is added for local docker networking access
  const localAliases = ['localhost', '127.0.0.1', 'prisma']
  const isPrivate =
    !shared &&
    !url.hostname.endsWith('db.cloud.prisma.sh') &&
    !localAliases.includes(url.hostname)
  const local = !shared && !isPrivate && !workspaceSlug
  
  if (isPrivate && !workspaceSlug) {
    workspaceSlug = getWorkspaceFromPrivateOrigin(url.origin)
  }

  return {
    clusterBaseUrl: url.origin,
    service,
    stage,
    local,
    isPrivate,
    shared,
    workspaceSlug,
    clusterName: getClusterName(url.origin),
  }
}
