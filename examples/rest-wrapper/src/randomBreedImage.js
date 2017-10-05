require('isomorphic-fetch')


module.exports = event => {

  const { breedName } = event.data
  const url = `https://dog.ceo/api/breed/${breedName}/images/random`

  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const randomBreedImageData = responseData.message
      const randomBreedImage = { url: randomBreedImageData }
      return { data: randomBreedImage }
    })
}
