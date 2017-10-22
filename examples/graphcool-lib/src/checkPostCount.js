import validator from 'validator'
import { fromEvent } from 'graphcool-lib'

export default async event => {

  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  const { id } = event.data.createUser

  const query = `
    query ($authorId: String!){
      _allPostsMeta(filter: {
        author: {
          id: $authorId
        } 
      }) {
        count
      }
    }
  `

  const variables = { authorId: id }
  const queryResponse = await api.request(query, variables)

  if (queryResponse.data._allPostsMeta.count > 100) {
    return {
      error: 'You can at most have 100 posts'
    }
  }

  return {
    data: event.data
  }
}