import {Resolver} from '../types'
import {readAuthConfig} from '../utils/file'
const debug = require('debug')('graphcool')

interface Props {

}

export default async(props: Props, resolver: Resolver): Promise<void> => {
  const authToken = readAuthConfig(resolver)
  debug(`authToken: ${JSON.stringify(authToken)}`)
}