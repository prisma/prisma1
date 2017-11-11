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
        const serializedError = JSON.stringify(destroyCircular(error, []))
        cb(serializedError)
      })
    } else {
      cb(null, promise)
    }
  } catch (e) {
    const serializedError = JSON.stringify(destroyCircular(e, []))
    cb(serializedError)
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

function destroyCircular(from, seen) {
  const to = Array.isArray(from) ? [] : {};

  seen.push(from);

  for (const key of Object.keys(from)) {
    const value = from[key];

    if (typeof value === 'function') {
      continue;
    }

    if (!value || typeof value !== 'object') {
      to[key] = value;
      continue;
    }

    if (seen.indexOf(from[key]) === -1) {
      to[key] = destroyCircular(from[key], seen.slice(0));
      continue;
    }

    to[key] = '[Circular]';
  }

  if (typeof from.name === 'string') {
    to.name = from.name;
  }

  if (typeof from.message === 'string') {
    to.message = from.message;
  }

  if (typeof from.stack === 'string') {
    to.stack = from.stack;
  }

  return to;
}
