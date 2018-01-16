require('isomorphic-fetch')


module.exports = event => {

  const { breedName, subBreedName } = event.data
  const url = `https://dog.ceo/api/breed/${breedName}/${subBreedName}/images/random`

  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const randomSubBreedImageData = responseData.message
      const randomSubBreedImage = { url: randomSubBreedImageData }
      return { data: randomSubBreedImage }
    })
}
