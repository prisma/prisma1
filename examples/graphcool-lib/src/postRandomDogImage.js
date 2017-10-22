import 'isomorphic-fetch'
import { fromEvent } from 'graphcool-lib'

const url = 'https://dog.ceo/api/breeds/image/random'

export default async event => {

  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  const result = await fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const randomDogImageData = responseData.message
      const saveRandomDogImage = { url: randomDogImageData }
      return { data: saveRandomDogImage }
    })
  console.log(`Result: ${JSON.stringify(result)}`)

  const mutation = `
    mutation ($imageUrl: String!, $authorId: String!) {
      createPost(
        authorId: $authorId 
        imageUrl: $imageUrl
      ) {
        id
      }
    }
  `

  const variables = { 
    imageUrl: result.data.url,
    authorId: event.data.authorId
  } 
  const mutationResponse = await api.request(mutation, variables)
  console.log(`Mutation response: ${JSON.stringify(mutationResponse)}`)

  return result
}