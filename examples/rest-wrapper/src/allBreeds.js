require('isomorphic-fetch')

const url = 'https://dog.ceo/api/breeds/list/all'

module.exports = () => {
  return fetch(url)
    .then(response => response.json())
    .then(responseData => {
      const breedsListData = responseData.message
      const breeds = []
      Object.keys(breedsListData).map(breedName => {
        const breed = {
          name: breedName,
          subBreeds: breedsListData[breedName]
        }
        breeds.push(breed)
      })
      return { data: breeds }
    })
}
