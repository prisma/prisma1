require('isomorphic-fetch')

module.exports = event => {

  const { breedName } = event.data
  const url = `https://dog.ceo/api/breed/${breedName}/images`

  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const breedImagesData = responseData.message
      const breedImages = []
      breedImagesData.map(url => {
        const breedImage = { url }
        breedImages.push(breedImage)
      })
      return { data: breedImages }
    })
}
