require('isomorphic-fetch')

module.exports = event => {

  const { breedName, subBreedName } = event.data

  const url = `https://dog.ceo/api/breed/${breedName}/${subBreedName}/images`

  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const subBreedImagesData = responseData.message
      const subBreedImages = []
      subBreedImagesData.map(url => {
        const subBreedImage = { url }
        subBreedImages.push(subBreedImage)
      })
      return { data: subBreedImages }
    })
}
