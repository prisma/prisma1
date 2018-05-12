import { invert } from 'lodash'

export const cloudApiEndpoint =
  process.env.CLOUD_API_ENDPOINT || 'https://api.cloud.prisma.sh'

export const clusterEndpointMap: { [key: string]: string } = {
  'prisma-eu1': 'https://eu1.prisma.sh',
  'prisma-us1': 'https://us1.prisma.sh',
}

export const clusterEndpointMapReverse: { [key: string]: string } = invert(
  clusterEndpointMap,
)
