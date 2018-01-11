import { fromEvent } from 'graphcool-lib'

const MAX_POSTS = 3

export default async event => {

  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  const { authorId } = event.data

  const query = `
    query ($authorId: ID!){
      _allPostsMeta(filter: {
        author: {
          id: $authorId
        } 
      }) {
        count
      }
    }
  `

  const variables = { authorId }
  const queryResponse = await api.request(query, variables)

  if (queryResponse._allPostsMeta.count >= MAX_POSTS) {
    return {
      error: `You can at most have ${MAX_POSTS} posts`
    }
  }

  return {
    data: event.data
  }
}