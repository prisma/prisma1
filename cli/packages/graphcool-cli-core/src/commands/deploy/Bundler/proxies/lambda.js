var fs = require('fs')
injectEnvironment()

exports.handle = function(event, ctx, cb) {
  var fn = require(getTargetFileName())
  fn = fn.default || fn
  executeFunction(fn, event, cb)
}

function executeFunction(fn, event, cb) {
  try {
    var promise = fn(event)
    if (promise && typeof promise.then === 'function') {
      promise.then(function (data) {
        cb(null, data)
      }).catch(function (error) {
        cb(error)
      })
    } else {
      cb(null, promise)
    }
  } catch (e) {
    cb(e)
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
