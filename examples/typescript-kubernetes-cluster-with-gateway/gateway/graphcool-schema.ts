import { HttpLink } from 'apollo-link-http'
import { makeRemoteExecutableSchema, introspectSchema } from 'graphql-tools'
import fetch from 'node-fetch'
import { GraphQLSchema } from 'graphql'

/*
  Utilize our exposed endpoint from the cluster for development. This will be swapped to an
  internal lookup when we deploy this gateway as a container. The id on the end will be generated
  when you first run `graphcool deploy` and is located inside of the .graphcoolrc file in the projects
  root directory
*/
const endpoint = `http://${process.env.KUBE_LOCAL_IP}:31000/simple/v1/cjaeirplq00wo0155cld1gtcu`
const link = new HttpLink({ uri: endpoint, fetch })

export default (): Promise<GraphQLSchema> => {
  return new Promise(async (resolve, reject) => {
    try {
      const schema = await introspectSchema(link)
      resolve(makeRemoteExecutableSchema({ schema, link }))
    } catch (e) {
      reject(e)
    }
  })
}
