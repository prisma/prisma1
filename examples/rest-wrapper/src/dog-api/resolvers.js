const resolvers = {
  Query: {
    subBreedImages: (parent, { breedName, subBreedName }) => {
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
    },
    randomSubBreedImage: (parent, { breedName, subBreedName }) => {
      const url = `https://dog.ceo/api/breed/${breedName}/${subBreedName}/images/random`
      return fetch(url)
        .then(response => response.json())
        .then(responseData => {
          const randomSubBreedImageData = responseData.message
          const randomSubBreedImage = { url: randomSubBreedImageData }
          return { data: randomSubBreedImage }
        })
    },
    randomDogImage: () => {
      const url = 'https://dog.ceo/api/breeds/image/random'
      return fetch(url)
        .then(response => response.json())
        .then(responseData => {
          const randomDogImageData = responseData.message
          const randomDogImage = { url: randomDogImageData }
          return { data: randomDogImage }
        })
    },
    randomBreedImage: (parent, { breedName }) => {
      const url = `https://dog.ceo/api/breed/${breedName}/images/random`
      return fetch(url)
        .then(response => response.json())
        .then(responseData => {
          const randomBreedImageData = responseData.message
          const randomBreedImage = { url: randomBreedImageData }
          return { data: randomBreedImage }
        })
    },
    breedImages: (parent, { breedName }) => {
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
    },
    allSubBreeds: (parent, { breedName }) => {
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
    },
    allBreeds: () => {
      const url = 'https://dog.ceo/api/breeds/list/all'
      return fetch(url)
        .then(response => response.json())
        .then(responseData => {
          const breedsListData = responseData.message
          const allBreeds = []
          Object.keys(breedsListData).map(breedName => {
            const breed = {
              name: breedName,
              subBreeds: breedsListData[breedName],
            }
            allBreeds.push(breed)
          })
          return { data: allBreeds }
        })
    },
  },
}

module.exports = {
  resolvers,
}
