module.exports = event => {
  // access the environment variable called `NAME`
  const name = event.data.name
  return {
    data: {
      message: `Hey ${name || 'World'}`
    }
  }
}