module.exports = event => {
  // access the environment variable called `NAME`
  const name = process.env['NAME'] || event.data.name
  return {
    data: {
      message: `Hello ${name || 'World'}`
    }
  }
}