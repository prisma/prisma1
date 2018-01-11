require('isomorphic-fetch')

module.exports = event => {

  const { breedName } = event.data
  const url = `https://dog.ceo/api/breed/${breedName}/list`

  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const allSubBreedsData = responseData.message
      const allSubBreeds = []
      allSubBreedsData.map(name => {
        const subBreed = { name }
        allSubBreeds.push(subBreed)
      })
      return { data: allSubBreeds }
    })
}