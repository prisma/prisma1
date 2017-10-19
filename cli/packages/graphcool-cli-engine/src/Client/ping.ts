import { Region } from '../types/common'
import * as cuid from 'scuid'
import {sum} from 'lodash'

async function runPing(url: string): Promise<number> {
  const pingUrl = async () => {
    const start = Date.now()

    if (process.env.NODE_ENV !== 'test') {
      await fetch(url)
    }

    return Date.now() - start
  }
  const pings = await Promise.all([0,0].map(pingUrl))

  return sum(pings) / pings.length
}

export const regions: Region[] = [
  'EU_WEST_1',
  'AP_NORTHEAST_1',
  'US_WEST_2',
]

export async function getFastestRegion(): Promise<Region> {
  const pingResults = await Promise.all(regions.map(async (region: Region) => {
    const ping = await runPing(getPingUrl(region))
    return {
      region, ping,
    }
  }))

  const fastestRegion: {region: Region, ping: number} = pingResults.reduce((min, curr) => {
    if (curr.ping < min.ping) {
      return curr
    }
    return min
  }, {region: 'EU_WEST_1', ping: Infinity})

  return fastestRegion.region
}

export const getDynamoUrl = (region: string) => `http://dynamodb.${region.toLowerCase().replace(/_/g, '-')}.amazonaws.com`

const getPingUrl = (region: string) => `${getDynamoUrl(region)}/ping?x=${cuid()}`
