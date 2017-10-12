var child = require('child_process')
var byline = require('./byline')
var fs = require('fs')
injectEnvironment()

var out = byline(process.stdin)
var old_stdout_write
var stdout = ''
var stderr = ''

intercept(function(data) {
	stdout += data
}, function(data) {
	stderr += data
})

var fn = require(getTargetFileName())
fn = fn.default || fn

out.on('data', function(line){
  const event = JSON.parse(line)
  executeFunction(fn, event, cb)
})

function cb(error, value) {
	const result = {
		error: error,
		value: value,
		stdout: stdout,
		stderr: stderr
	}
  old_stdout_write(JSON.stringify(result))
}

function executeFunction(fn, event, cb) {
  try {
    var promise = fn(event)
    if (typeof promise.then === 'function') {
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
  return __filename.slice(0, __filename.length - 7) + '.js'
}

function getEnvFileName() {
  return __filename.slice(0, __filename.length - 7) + '.env.json'
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

function intercept (stdoutIntercept, stderrIntercept) {
	stderrIntercept = stderrIntercept || stdoutIntercept;

	old_stdout_write = process.stdout.write;
	var old_stderr_write = process.stderr.write;

	process.stdout.write = (function(write) {
		return function(string, encoding, fd) {
			var args = toArray(arguments);
			args[0] = interceptor( string, stdoutIntercept );
			write.apply(process.stdout, args);
		};
	}(process.stdout.write));

	process.stderr.write = (function(write) {
		return function(string, encoding, fd) {
			var args = toArray(arguments);
			args[0] = interceptor( string, stderrIntercept );
			write.apply(process.stderr, args);
		};
	}(process.stderr.write));

	function interceptor(string, callback) {
		// only intercept the string
		var result = callback(string);
		if (typeof result == 'string') {
			string = result.replace( /\n$/ , '' ) + (result && (/\n$/).test( string ) ? '\n' : '');
		}
		return string;
	}

	// puts back to original
	return function unhook() {
		process.stdout.write = old_stdout_write;
		process.stderr.write = old_stderr_write;
	};

}
