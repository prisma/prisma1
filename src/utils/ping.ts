import { Region } from '../types'
import * as cuid from 'cuid'
import {sum} from 'lodash'

async function runPing(url: string): Promise<number> {
  const pingUrl = async () => {
    const start = Date.now()

    await fetch(url)

    return Date.now() - start
  }
  const pings = await Promise.all([0,0].map(pingUrl))

  return sum(pings) / pings.length
}

export const regions: Region[] = [
  'eu_west_1',
  'ap_northeast_1',
  'us_west_2',
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
  }, {region: 'eu_west_1', ping: Infinity})

  return fastestRegion.region
}

export const getDynamoUrl = (region: string) => `http://dynamodb.${region.replace(/_/g, '-')}.amazonaws.com`

const getPingUrl = (region: string) => `${getDynamoUrl(region)}/ping?x=${cuid()}`
