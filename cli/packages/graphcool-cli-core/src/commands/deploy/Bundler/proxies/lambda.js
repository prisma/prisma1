var fs = require('fs')
injectEnvironment()

module.exports = function (event, context, callback) {
  var fn = require(getTargetFileName())
  fn = fn.default || fn
  var promise = fn(event)
  if (typeof promise.then === 'function') {
    promise.then(function (data) {
      callback(null, data)
    }).catch(function (error) {
      callback(error)
    })
  } else {
    return promise
  }
}

function getTargetFileName() {
  return __filename.slice(0, __filename.length - 10) + '.js'
}

function getEnvFileName() {
  return __filename.slice(0, __filename.length - 10) + '.env.json'
}

function injectEnvironment() {
  var envFileName = getEnvFileName()
  if(fs.existsSync(envFileName)) {
    try {
      var file = fs.readFileSync(envFileName, 'utf-8')
      var json = JSON.parse(file)
      if (json) {
        Object.assign(process.env, json)
      }
    } catch (e) {
      // noop
    }
  }
}