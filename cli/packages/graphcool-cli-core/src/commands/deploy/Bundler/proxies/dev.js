var child = require('child_process')
var byline = require('./byline')

var out = byline(process.stdin)

out.on('data', function(line){

})
