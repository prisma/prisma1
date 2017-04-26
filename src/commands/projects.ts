import {Resolver} from '../types'
import {readAuthConfig} from '../utils/file'
import {fetchProjects} from '../api/api'
const debug = require('debug')('graphcool')

interface Props {

}

export default async(props: Props, resolver: Resolver): Promise<void> => {

  const projects = await fetchProjects(resolver)



}