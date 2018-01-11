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
      const postRandomDogImage = { url: randomDogImageData }
      return { data: postRandomDogImage }
    })

  const mutation = `
    mutation ($imageURL: String!, $authorId: ID!) {
      createPost(
        authorId: $authorId 
        imageURL: $imageURL
      ) {
        id
      }
    }
  `

  const variables = { 
    imageURL: result.data.url,
    authorId: event.data.authorId
  } 

  try {
    await api.request(mutation, variables)  
  } catch (error) {
    console.log(`Mutation failed: ${JSON.stringify(error)}`)
    return {
      error: error.response.errors[0].functionError
    }
  }

  return result
}