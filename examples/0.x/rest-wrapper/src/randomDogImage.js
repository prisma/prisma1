require('isomorphic-fetch')

const url = 'https://dog.ceo/api/breeds/image/random'

module.exports = () => {
  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const randomDogImageData = responseData.message
      const randomDogImage = { url: randomDogImageData }
      return { data: randomDogImage }
    })
}
