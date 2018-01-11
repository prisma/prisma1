module.exports = event => {
  const name = event.data.name
  return {
    data: {
      message: `Hello ${name || 'World'}`
    }
  }
}