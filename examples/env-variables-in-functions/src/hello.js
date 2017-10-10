module.exports = event => {
  // Access the environment variable called `NAME` that's
  // defined for this function inside `graphcool.yml`.
  const name = event.data.name || process.env['NAME']
  return {
    data: {
      message: `Hello ${name || 'World'}`
    }
  }
}