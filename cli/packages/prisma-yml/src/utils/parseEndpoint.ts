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

const getWorkspaceFromPrivateOrigin = (origin: string) =>
  origin.split('_')[1].split('.')[0]

const isLocal = origin =>
  origin.includes('localhost') || origin.includes('127.0.0.1')

export function parseEndpoint(
  endpoint: string,
): {
  service: string
  clusterBaseUrl: string
  stage: string
  isPrivate: boolean
  local: boolean
  shared: boolean
  workspaceSlug: string | undefined
  clusterName: string
} {
  const url = new URL(endpoint)
  const splittedPath = url.pathname.split('/')
  const shared = ['eu1.prisma.sh', 'us2.prisma.sh'].includes(url.host)
  const isPrivate =
    !shared &&
    url.host.endsWith('prisma.sh') &&
    !url.host.endsWith('db.cloud.prisma.sh')
  const local = !shared && !isPrivate
  // assuming, that the pathname always starts with a leading /, we always can ignore the first element of the split array
  const service =
    splittedPath.length > 3 ? splittedPath[2] : splittedPath[1] || 'default'
  const stage =
    splittedPath.length > 3 ? splittedPath[3] : splittedPath[2] || 'default'
  let workspaceSlug = splittedPath.length > 3 ? splittedPath[1] : undefined
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
