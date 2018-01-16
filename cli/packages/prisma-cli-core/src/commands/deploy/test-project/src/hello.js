module.exports = event => {
  return {
    data: {
      message: `Hello ${event.data.name || 'World'}`
    }
  }
}