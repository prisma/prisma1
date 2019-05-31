module.exports =
/******/ (function(modules, runtime) { // webpackBootstrap
/******/ 	"use strict";
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/
/******/ 	// the startup function
/******/ 	function startup() {
/******/ 		// Load entry module and return exports
/******/ 		return __webpack_require__(791);
/******/ 	};
/******/ 	// initialize runtime
/******/ 	runtime(__webpack_require__);
/******/
/******/ 	// run startup
/******/ 	return startup();
/******/ })
/************************************************************************/
/******/ ({

/***/ 15:
/***/ (function(module) {

module.exports = require("encoding");

/***/ }),

/***/ 50:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const chalk_1 = __importDefault(__webpack_require__(936));
const strip_ansi_1 = __importDefault(__webpack_require__(656));
const deep_set_1 = __webpack_require__(953);
const stringifyObject_1 = __importDefault(__webpack_require__(763));
const DIM_TOKEN = '@@__DIM_POINTER__@@';
function printJsonWithErrors(ast, keyPaths, valuePaths, missingItems = []) {
    let obj = ast;
    for (const { path, type } of missingItems) {
        obj = deep_set_1.deepSet(obj, path, type);
    }
    return stringifyObject_1.default(obj, {
        indent: '  ',
        transformLine: ({ indent, key, value, stringifiedValue, eol, path }) => {
            const dottedPath = path.join('.');
            const keyError = keyPaths.includes(dottedPath);
            const valueError = valuePaths.includes(dottedPath);
            const missingItem = missingItems.find(item => item.path === dottedPath);
            let valueStr = stringifiedValue;
            if (missingItem) {
                // trim away the '' from the string
                if (typeof value === 'string') {
                    valueStr = valueStr.slice(1, valueStr.length - 1);
                }
                const isRequiredStr = missingItem.isRequired ? '' : '?';
                let output = chalk_1.default.greenBright(prefixLines(key + isRequiredStr + ': ' + valueStr + eol, indent, '+'));
                if (!missingItem.isRequired) {
                    output = chalk_1.default.dim(output);
                }
                return output;
            }
            else {
                const isOnMissingItemPath = missingItems.some(item => dottedPath.startsWith(item.path));
                const isOptional = key[key.length - 2] === '?';
                if (isOptional) {
                    key = key.slice(1, key.length - 1);
                }
                if (isOptional && typeof value === 'object' && value !== null) {
                    valueStr = valueStr
                        .split('\n')
                        .map((line, index, arr) => (index === arr.length - 1 ? line + DIM_TOKEN : line))
                        .join('\n');
                }
                if (isOnMissingItemPath && typeof value === 'string') {
                    valueStr = valueStr.slice(1, valueStr.length - 1);
                    if (!isOptional) {
                        valueStr = chalk_1.default.bold(valueStr);
                    }
                }
                if ((typeof value !== 'object' || value === null) && !valueError && !isOnMissingItemPath) {
                    valueStr = chalk_1.default.dim(valueStr);
                }
                const keyStr = keyError ? chalk_1.default.redBright(key) : key;
                valueStr = valueError ? chalk_1.default.redBright(valueStr) : valueStr;
                // valueStr can be multiple lines if it's an object
                let output = indent + keyStr + ': ' + valueStr + (isOnMissingItemPath ? eol : chalk_1.default.dim(eol));
                // if there is an error, add the scribble lines
                // 3 options:
                // error in key, but not in value
                // error in value, but not in key
                // error in both
                if (keyError || valueError) {
                    const lines = output.split('\n');
                    const keyLength = String(key).length;
                    const keyScribbles = keyError ? chalk_1.default.redBright('~'.repeat(keyLength)) : ' '.repeat(keyLength);
                    const valueLength = valueError ? getValueLength(indent, key, value, stringifiedValue) : 0;
                    const hideValueScribbles = Boolean(valueError && (typeof value === 'object' && value !== null));
                    const valueScribbles = valueError ? '  ' + chalk_1.default.redBright('~'.repeat(valueLength)) : '';
                    // Either insert both keyScribles and valueScribbles in one line
                    if (keyScribbles && keyScribbles.length > 0 && !hideValueScribbles) {
                        lines.splice(1, 0, indent + keyScribbles + valueScribbles);
                    }
                    // or the valueScribbles for a multiline string
                    if (keyScribbles && keyScribbles.length > 0 && hideValueScribbles) {
                        lines.splice(lines.length - 1, 0, indent.slice(0, indent.length - 2) + valueScribbles);
                    }
                    output = lines.join('\n');
                }
                return output;
            }
        },
    });
}
exports.printJsonWithErrors = printJsonWithErrors;
function getValueLength(indent, key, value, stringifiedValue) {
    if (value === null) {
        return 4;
    }
    if (typeof value === 'string') {
        return value.length + 2; // +2 for the quotes
    }
    if (typeof value === 'object') {
        return getLongestLine(`${key}: ${strip_ansi_1.default(stringifiedValue)}`) - indent.length;
    }
    return String(value).length;
}
function getLongestLine(str) {
    return str.split('\n').reduce((max, curr) => (curr.length > max ? curr.length : max), 0);
}
function prefixLines(str, indent, prefix) {
    return str
        .split('\n')
        .map((line, index, arr) => index === 0 ? prefix + indent.slice(1) + line : index < arr.length - 1 ? prefix + line.slice(1) : line)
        .map(line => {
        // we need to use a special token to "mark" a line a "to be dimmed", as chalk (or rather ansi) doesn't allow nesting of dimmed & colored content
        return strip_ansi_1.default(line).includes(DIM_TOKEN)
            ? chalk_1.default.dim(line.replace(DIM_TOKEN, ''))
            : line.includes('?')
                ? chalk_1.default.dim(line)
                : line;
    })
        .join('\n');
}


/***/ }),

/***/ 56:
/***/ (function(module, exports, __webpack_require__) {

var nodeFetch = __webpack_require__(816)
var realFetch = nodeFetch.default || nodeFetch

var fetch = function (url, options) {
  // Support schemaless URIs on the server for parity with the browser.
  // Ex: //github.com/ -> https://github.com/
  if (/^\/\//.test(url)) {
    url = 'https:' + url
  }
  return realFetch.call(this, url, options)
}

module.exports = exports = fetch
exports.fetch = fetch
exports.Headers = nodeFetch.Headers
exports.Request = nodeFetch.Request
exports.Response = nodeFetch.Response

// Needed for TypeScript consumers without esModuleInterop.
exports.default = fetch


/***/ }),

/***/ 64:
/***/ (function(module) {

module.exports = require("util");

/***/ }),

/***/ 66:
/***/ (function(module) {

module.exports = require("fs");

/***/ }),

/***/ 68:
/***/ (function(module) {

"use strict";

module.exports = (flag, argv) => {
	argv = argv || process.argv;
	const prefix = flag.startsWith('-') ? '' : (flag.length === 1 ? '-' : '--');
	const pos = argv.indexOf(prefix + flag);
	const terminatorPos = argv.indexOf('--');
	return pos !== -1 && (terminatorPos === -1 ? true : pos < terminatorPos);
};


/***/ }),

/***/ 77:
/***/ (function(module) {

module.exports = require("url");

/***/ }),

/***/ 79:
/***/ (function(module, __unusedexports, __webpack_require__) {

/**
 * Detect Electron renderer / nwjs process, which is node, but we should
 * treat as a browser.
 */

if (typeof process === 'undefined' || process.type === 'renderer' || process.browser === true || process.__nwjs) {
	module.exports = __webpack_require__(620);
} else {
	module.exports = __webpack_require__(828);
}


/***/ }),

/***/ 86:
/***/ (function(module) {

/**
 * Helpers.
 */

var s = 1000;
var m = s * 60;
var h = m * 60;
var d = h * 24;
var w = d * 7;
var y = d * 365.25;

/**
 * Parse or format the given `val`.
 *
 * Options:
 *
 *  - `long` verbose formatting [false]
 *
 * @param {String|Number} val
 * @param {Object} [options]
 * @throws {Error} throw an error if val is not a non-empty string or a number
 * @return {String|Number}
 * @api public
 */

module.exports = function(val, options) {
  options = options || {};
  var type = typeof val;
  if (type === 'string' && val.length > 0) {
    return parse(val);
  } else if (type === 'number' && isNaN(val) === false) {
    return options.long ? fmtLong(val) : fmtShort(val);
  }
  throw new Error(
    'val is not a non-empty string or a valid number. val=' +
      JSON.stringify(val)
  );
};

/**
 * Parse the given `str` and return milliseconds.
 *
 * @param {String} str
 * @return {Number}
 * @api private
 */

function parse(str) {
  str = String(str);
  if (str.length > 100) {
    return;
  }
  var match = /^((?:\d+)?\-?\d?\.?\d+) *(milliseconds?|msecs?|ms|seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h|days?|d|weeks?|w|years?|yrs?|y)?$/i.exec(
    str
  );
  if (!match) {
    return;
  }
  var n = parseFloat(match[1]);
  var type = (match[2] || 'ms').toLowerCase();
  switch (type) {
    case 'years':
    case 'year':
    case 'yrs':
    case 'yr':
    case 'y':
      return n * y;
    case 'weeks':
    case 'week':
    case 'w':
      return n * w;
    case 'days':
    case 'day':
    case 'd':
      return n * d;
    case 'hours':
    case 'hour':
    case 'hrs':
    case 'hr':
    case 'h':
      return n * h;
    case 'minutes':
    case 'minute':
    case 'mins':
    case 'min':
    case 'm':
      return n * m;
    case 'seconds':
    case 'second':
    case 'secs':
    case 'sec':
    case 's':
      return n * s;
    case 'milliseconds':
    case 'millisecond':
    case 'msecs':
    case 'msec':
    case 'ms':
      return n;
    default:
      return undefined;
  }
}

/**
 * Short format for `ms`.
 *
 * @param {Number} ms
 * @return {String}
 * @api private
 */

function fmtShort(ms) {
  var msAbs = Math.abs(ms);
  if (msAbs >= d) {
    return Math.round(ms / d) + 'd';
  }
  if (msAbs >= h) {
    return Math.round(ms / h) + 'h';
  }
  if (msAbs >= m) {
    return Math.round(ms / m) + 'm';
  }
  if (msAbs >= s) {
    return Math.round(ms / s) + 's';
  }
  return ms + 'ms';
}

/**
 * Long format for `ms`.
 *
 * @param {Number} ms
 * @return {String}
 * @api private
 */

function fmtLong(ms) {
  var msAbs = Math.abs(ms);
  if (msAbs >= d) {
    return plural(ms, msAbs, d, 'day');
  }
  if (msAbs >= h) {
    return plural(ms, msAbs, h, 'hour');
  }
  if (msAbs >= m) {
    return plural(ms, msAbs, m, 'minute');
  }
  if (msAbs >= s) {
    return plural(ms, msAbs, s, 'second');
  }
  return ms + ' ms';
}

/**
 * Pluralization helper.
 */

function plural(ms, msAbs, n, name) {
  var isPlural = msAbs >= n * 1.5;
  return Math.round(ms / n) + ' ' + name + (isPlural ? 's' : '');
}


/***/ }),

/***/ 96:
/***/ (function(module) {

"use strict";


module.exports = {
	"aliceblue": [240, 248, 255],
	"antiquewhite": [250, 235, 215],
	"aqua": [0, 255, 255],
	"aquamarine": [127, 255, 212],
	"azure": [240, 255, 255],
	"beige": [245, 245, 220],
	"bisque": [255, 228, 196],
	"black": [0, 0, 0],
	"blanchedalmond": [255, 235, 205],
	"blue": [0, 0, 255],
	"blueviolet": [138, 43, 226],
	"brown": [165, 42, 42],
	"burlywood": [222, 184, 135],
	"cadetblue": [95, 158, 160],
	"chartreuse": [127, 255, 0],
	"chocolate": [210, 105, 30],
	"coral": [255, 127, 80],
	"cornflowerblue": [100, 149, 237],
	"cornsilk": [255, 248, 220],
	"crimson": [220, 20, 60],
	"cyan": [0, 255, 255],
	"darkblue": [0, 0, 139],
	"darkcyan": [0, 139, 139],
	"darkgoldenrod": [184, 134, 11],
	"darkgray": [169, 169, 169],
	"darkgreen": [0, 100, 0],
	"darkgrey": [169, 169, 169],
	"darkkhaki": [189, 183, 107],
	"darkmagenta": [139, 0, 139],
	"darkolivegreen": [85, 107, 47],
	"darkorange": [255, 140, 0],
	"darkorchid": [153, 50, 204],
	"darkred": [139, 0, 0],
	"darksalmon": [233, 150, 122],
	"darkseagreen": [143, 188, 143],
	"darkslateblue": [72, 61, 139],
	"darkslategray": [47, 79, 79],
	"darkslategrey": [47, 79, 79],
	"darkturquoise": [0, 206, 209],
	"darkviolet": [148, 0, 211],
	"deeppink": [255, 20, 147],
	"deepskyblue": [0, 191, 255],
	"dimgray": [105, 105, 105],
	"dimgrey": [105, 105, 105],
	"dodgerblue": [30, 144, 255],
	"firebrick": [178, 34, 34],
	"floralwhite": [255, 250, 240],
	"forestgreen": [34, 139, 34],
	"fuchsia": [255, 0, 255],
	"gainsboro": [220, 220, 220],
	"ghostwhite": [248, 248, 255],
	"gold": [255, 215, 0],
	"goldenrod": [218, 165, 32],
	"gray": [128, 128, 128],
	"green": [0, 128, 0],
	"greenyellow": [173, 255, 47],
	"grey": [128, 128, 128],
	"honeydew": [240, 255, 240],
	"hotpink": [255, 105, 180],
	"indianred": [205, 92, 92],
	"indigo": [75, 0, 130],
	"ivory": [255, 255, 240],
	"khaki": [240, 230, 140],
	"lavender": [230, 230, 250],
	"lavenderblush": [255, 240, 245],
	"lawngreen": [124, 252, 0],
	"lemonchiffon": [255, 250, 205],
	"lightblue": [173, 216, 230],
	"lightcoral": [240, 128, 128],
	"lightcyan": [224, 255, 255],
	"lightgoldenrodyellow": [250, 250, 210],
	"lightgray": [211, 211, 211],
	"lightgreen": [144, 238, 144],
	"lightgrey": [211, 211, 211],
	"lightpink": [255, 182, 193],
	"lightsalmon": [255, 160, 122],
	"lightseagreen": [32, 178, 170],
	"lightskyblue": [135, 206, 250],
	"lightslategray": [119, 136, 153],
	"lightslategrey": [119, 136, 153],
	"lightsteelblue": [176, 196, 222],
	"lightyellow": [255, 255, 224],
	"lime": [0, 255, 0],
	"limegreen": [50, 205, 50],
	"linen": [250, 240, 230],
	"magenta": [255, 0, 255],
	"maroon": [128, 0, 0],
	"mediumaquamarine": [102, 205, 170],
	"mediumblue": [0, 0, 205],
	"mediumorchid": [186, 85, 211],
	"mediumpurple": [147, 112, 219],
	"mediumseagreen": [60, 179, 113],
	"mediumslateblue": [123, 104, 238],
	"mediumspringgreen": [0, 250, 154],
	"mediumturquoise": [72, 209, 204],
	"mediumvioletred": [199, 21, 133],
	"midnightblue": [25, 25, 112],
	"mintcream": [245, 255, 250],
	"mistyrose": [255, 228, 225],
	"moccasin": [255, 228, 181],
	"navajowhite": [255, 222, 173],
	"navy": [0, 0, 128],
	"oldlace": [253, 245, 230],
	"olive": [128, 128, 0],
	"olivedrab": [107, 142, 35],
	"orange": [255, 165, 0],
	"orangered": [255, 69, 0],
	"orchid": [218, 112, 214],
	"palegoldenrod": [238, 232, 170],
	"palegreen": [152, 251, 152],
	"paleturquoise": [175, 238, 238],
	"palevioletred": [219, 112, 147],
	"papayawhip": [255, 239, 213],
	"peachpuff": [255, 218, 185],
	"peru": [205, 133, 63],
	"pink": [255, 192, 203],
	"plum": [221, 160, 221],
	"powderblue": [176, 224, 230],
	"purple": [128, 0, 128],
	"rebeccapurple": [102, 51, 153],
	"red": [255, 0, 0],
	"rosybrown": [188, 143, 143],
	"royalblue": [65, 105, 225],
	"saddlebrown": [139, 69, 19],
	"salmon": [250, 128, 114],
	"sandybrown": [244, 164, 96],
	"seagreen": [46, 139, 87],
	"seashell": [255, 245, 238],
	"sienna": [160, 82, 45],
	"silver": [192, 192, 192],
	"skyblue": [135, 206, 235],
	"slateblue": [106, 90, 205],
	"slategray": [112, 128, 144],
	"slategrey": [112, 128, 144],
	"snow": [255, 250, 250],
	"springgreen": [0, 255, 127],
	"steelblue": [70, 130, 180],
	"tan": [210, 180, 140],
	"teal": [0, 128, 128],
	"thistle": [216, 191, 216],
	"tomato": [255, 99, 71],
	"turquoise": [64, 224, 208],
	"violet": [238, 130, 238],
	"wheat": [245, 222, 179],
	"white": [255, 255, 255],
	"whitesmoke": [245, 245, 245],
	"yellow": [255, 255, 0],
	"yellowgreen": [154, 205, 50]
};


/***/ }),

/***/ 99:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";

const os = __webpack_require__(431);
const hasFlag = __webpack_require__(68);

const env = process.env;

let forceColor;
if (hasFlag('no-color') ||
	hasFlag('no-colors') ||
	hasFlag('color=false')) {
	forceColor = false;
} else if (hasFlag('color') ||
	hasFlag('colors') ||
	hasFlag('color=true') ||
	hasFlag('color=always')) {
	forceColor = true;
}
if ('FORCE_COLOR' in env) {
	forceColor = env.FORCE_COLOR.length === 0 || parseInt(env.FORCE_COLOR, 10) !== 0;
}

function translateLevel(level) {
	if (level === 0) {
		return false;
	}

	return {
		level,
		hasBasic: true,
		has256: level >= 2,
		has16m: level >= 3
	};
}

function supportsColor(stream) {
	if (forceColor === false) {
		return 0;
	}

	if (hasFlag('color=16m') ||
		hasFlag('color=full') ||
		hasFlag('color=truecolor')) {
		return 3;
	}

	if (hasFlag('color=256')) {
		return 2;
	}

	if (stream && !stream.isTTY && forceColor !== true) {
		return 0;
	}

	const min = forceColor ? 1 : 0;

	if (process.platform === 'win32') {
		// Node.js 7.5.0 is the first version of Node.js to include a patch to
		// libuv that enables 256 color output on Windows. Anything earlier and it
		// won't work. However, here we target Node.js 8 at minimum as it is an LTS
		// release, and Node.js 7 is not. Windows 10 build 10586 is the first Windows
		// release that supports 256 colors. Windows 10 build 14931 is the first release
		// that supports 16m/TrueColor.
		const osRelease = os.release().split('.');
		if (
			Number(process.versions.node.split('.')[0]) >= 8 &&
			Number(osRelease[0]) >= 10 &&
			Number(osRelease[2]) >= 10586
		) {
			return Number(osRelease[2]) >= 14931 ? 3 : 2;
		}

		return 1;
	}

	if ('CI' in env) {
		if (['TRAVIS', 'CIRCLECI', 'APPVEYOR', 'GITLAB_CI'].some(sign => sign in env) || env.CI_NAME === 'codeship') {
			return 1;
		}

		return min;
	}

	if ('TEAMCITY_VERSION' in env) {
		return /^(9\.(0*[1-9]\d*)\.|\d{2,}\.)/.test(env.TEAMCITY_VERSION) ? 1 : 0;
	}

	if (env.COLORTERM === 'truecolor') {
		return 3;
	}

	if ('TERM_PROGRAM' in env) {
		const version = parseInt((env.TERM_PROGRAM_VERSION || '').split('.')[0], 10);

		switch (env.TERM_PROGRAM) {
			case 'iTerm.app':
				return version >= 3 ? 3 : 2;
			case 'Apple_Terminal':
				return 2;
			// No default
		}
	}

	if (/-256(color)?$/i.test(env.TERM)) {
		return 2;
	}

	if (/^screen|^xterm|^vt100|^vt220|^rxvt|color|ansi|cygwin|linux/i.test(env.TERM)) {
		return 1;
	}

	if ('COLORTERM' in env) {
		return 1;
	}

	if (env.TERM === 'dumb') {
		return min;
	}

	return min;
}

function getSupportLevel(stream) {
	const level = supportsColor(stream);
	return translateLevel(level);
}

module.exports = {
	supportsColor: getSupportLevel,
	stdout: getSupportLevel(process.stdout),
	stderr: getSupportLevel(process.stderr)
};


/***/ }),

/***/ 101:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";

const os = __webpack_require__(431);
const hasFlag = __webpack_require__(834);

const {env} = process;

let forceColor;
if (hasFlag('no-color') ||
	hasFlag('no-colors') ||
	hasFlag('color=false') ||
	hasFlag('color=never')) {
	forceColor = 0;
} else if (hasFlag('color') ||
	hasFlag('colors') ||
	hasFlag('color=true') ||
	hasFlag('color=always')) {
	forceColor = 1;
}
if ('FORCE_COLOR' in env) {
	if (env.FORCE_COLOR === true || env.FORCE_COLOR === 'true') {
		forceColor = 1;
	} else if (env.FORCE_COLOR === false || env.FORCE_COLOR === 'false') {
		forceColor = 0;
	} else {
		forceColor = env.FORCE_COLOR.length === 0 ? 1 : Math.min(parseInt(env.FORCE_COLOR, 10), 3);
	}
}

function translateLevel(level) {
	if (level === 0) {
		return false;
	}

	return {
		level,
		hasBasic: true,
		has256: level >= 2,
		has16m: level >= 3
	};
}

function supportsColor(stream) {
	if (forceColor === 0) {
		return 0;
	}

	if (hasFlag('color=16m') ||
		hasFlag('color=full') ||
		hasFlag('color=truecolor')) {
		return 3;
	}

	if (hasFlag('color=256')) {
		return 2;
	}

	if (stream && !stream.isTTY && forceColor === undefined) {
		return 0;
	}

	const min = forceColor || 0;

	if (env.TERM === 'dumb') {
		return min;
	}

	if (process.platform === 'win32') {
		// Node.js 7.5.0 is the first version of Node.js to include a patch to
		// libuv that enables 256 color output on Windows. Anything earlier and it
		// won't work. However, here we target Node.js 8 at minimum as it is an LTS
		// release, and Node.js 7 is not. Windows 10 build 10586 is the first Windows
		// release that supports 256 colors. Windows 10 build 14931 is the first release
		// that supports 16m/TrueColor.
		const osRelease = os.release().split('.');
		if (
			Number(process.versions.node.split('.')[0]) >= 8 &&
			Number(osRelease[0]) >= 10 &&
			Number(osRelease[2]) >= 10586
		) {
			return Number(osRelease[2]) >= 14931 ? 3 : 2;
		}

		return 1;
	}

	if ('CI' in env) {
		if (['TRAVIS', 'CIRCLECI', 'APPVEYOR', 'GITLAB_CI'].some(sign => sign in env) || env.CI_NAME === 'codeship') {
			return 1;
		}

		return min;
	}

	if ('TEAMCITY_VERSION' in env) {
		return /^(9\.(0*[1-9]\d*)\.|\d{2,}\.)/.test(env.TEAMCITY_VERSION) ? 1 : 0;
	}

	if (env.COLORTERM === 'truecolor') {
		return 3;
	}

	if ('TERM_PROGRAM' in env) {
		const version = parseInt((env.TERM_PROGRAM_VERSION || '').split('.')[0], 10);

		switch (env.TERM_PROGRAM) {
			case 'iTerm.app':
				return version >= 3 ? 3 : 2;
			case 'Apple_Terminal':
				return 2;
			// No default
		}
	}

	if (/-256(color)?$/i.test(env.TERM)) {
		return 2;
	}

	if (/^screen|^xterm|^vt100|^vt220|^rxvt|color|ansi|cygwin|linux/i.test(env.TERM)) {
		return 1;
	}

	if ('COLORTERM' in env) {
		return 1;
	}

	return min;
}

function getSupportLevel(stream) {
	const level = supportsColor(stream);
	return translateLevel(level);
}

module.exports = {
	supportsColor: getSupportLevel,
	stdout: getSupportLevel(process.stdout),
	stderr: getSupportLevel(process.stderr)
};


/***/ }),

/***/ 103:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";
/* module decorator */ module = __webpack_require__.nmd(module);

const colorConvert = __webpack_require__(275);

const wrapAnsi16 = (fn, offset) => function () {
	const code = fn.apply(colorConvert, arguments);
	return `\u001B[${code + offset}m`;
};

const wrapAnsi256 = (fn, offset) => function () {
	const code = fn.apply(colorConvert, arguments);
	return `\u001B[${38 + offset};5;${code}m`;
};

const wrapAnsi16m = (fn, offset) => function () {
	const rgb = fn.apply(colorConvert, arguments);
	return `\u001B[${38 + offset};2;${rgb[0]};${rgb[1]};${rgb[2]}m`;
};

function assembleStyles() {
	const codes = new Map();
	const styles = {
		modifier: {
			reset: [0, 0],
			// 21 isn't widely supported and 22 does the same thing
			bold: [1, 22],
			dim: [2, 22],
			italic: [3, 23],
			underline: [4, 24],
			inverse: [7, 27],
			hidden: [8, 28],
			strikethrough: [9, 29]
		},
		color: {
			black: [30, 39],
			red: [31, 39],
			green: [32, 39],
			yellow: [33, 39],
			blue: [34, 39],
			magenta: [35, 39],
			cyan: [36, 39],
			white: [37, 39],
			gray: [90, 39],

			// Bright color
			redBright: [91, 39],
			greenBright: [92, 39],
			yellowBright: [93, 39],
			blueBright: [94, 39],
			magentaBright: [95, 39],
			cyanBright: [96, 39],
			whiteBright: [97, 39]
		},
		bgColor: {
			bgBlack: [40, 49],
			bgRed: [41, 49],
			bgGreen: [42, 49],
			bgYellow: [43, 49],
			bgBlue: [44, 49],
			bgMagenta: [45, 49],
			bgCyan: [46, 49],
			bgWhite: [47, 49],

			// Bright color
			bgBlackBright: [100, 49],
			bgRedBright: [101, 49],
			bgGreenBright: [102, 49],
			bgYellowBright: [103, 49],
			bgBlueBright: [104, 49],
			bgMagentaBright: [105, 49],
			bgCyanBright: [106, 49],
			bgWhiteBright: [107, 49]
		}
	};

	// Fix humans
	styles.color.grey = styles.color.gray;

	for (const groupName of Object.keys(styles)) {
		const group = styles[groupName];

		for (const styleName of Object.keys(group)) {
			const style = group[styleName];

			styles[styleName] = {
				open: `\u001B[${style[0]}m`,
				close: `\u001B[${style[1]}m`
			};

			group[styleName] = styles[styleName];

			codes.set(style[0], style[1]);
		}

		Object.defineProperty(styles, groupName, {
			value: group,
			enumerable: false
		});

		Object.defineProperty(styles, 'codes', {
			value: codes,
			enumerable: false
		});
	}

	const ansi2ansi = n => n;
	const rgb2rgb = (r, g, b) => [r, g, b];

	styles.color.close = '\u001B[39m';
	styles.bgColor.close = '\u001B[49m';

	styles.color.ansi = {
		ansi: wrapAnsi16(ansi2ansi, 0)
	};
	styles.color.ansi256 = {
		ansi256: wrapAnsi256(ansi2ansi, 0)
	};
	styles.color.ansi16m = {
		rgb: wrapAnsi16m(rgb2rgb, 0)
	};

	styles.bgColor.ansi = {
		ansi: wrapAnsi16(ansi2ansi, 10)
	};
	styles.bgColor.ansi256 = {
		ansi256: wrapAnsi256(ansi2ansi, 10)
	};
	styles.bgColor.ansi16m = {
		rgb: wrapAnsi16m(rgb2rgb, 10)
	};

	for (let key of Object.keys(colorConvert)) {
		if (typeof colorConvert[key] !== 'object') {
			continue;
		}

		const suite = colorConvert[key];

		if (key === 'ansi16') {
			key = 'ansi';
		}

		if ('ansi16' in suite) {
			styles.color.ansi[key] = wrapAnsi16(suite.ansi16, 0);
			styles.bgColor.ansi[key] = wrapAnsi16(suite.ansi16, 10);
		}

		if ('ansi256' in suite) {
			styles.color.ansi256[key] = wrapAnsi256(suite.ansi256, 0);
			styles.bgColor.ansi256[key] = wrapAnsi256(suite.ansi256, 10);
		}

		if ('rgb' in suite) {
			styles.color.ansi16m[key] = wrapAnsi16m(suite.rgb, 0);
			styles.bgColor.ansi16m[key] = wrapAnsi16m(suite.rgb, 10);
		}
	}

	return styles;
}

// Make the export immutable
Object.defineProperty(module, 'exports', {
	enumerable: true,
	get: assembleStyles
});


/***/ }),

/***/ 141:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const util_1 = __webpack_require__(64);
const child_process_1 = __webpack_require__(204);
const fs = __importStar(__webpack_require__(66));
const path = __importStar(__webpack_require__(589));
const net = __importStar(__webpack_require__(193));
const debug_1 = __importDefault(__webpack_require__(197));
const cross_fetch_1 = __importDefault(__webpack_require__(56));
const getInternalDatamodelJson_1 = __webpack_require__(328);
const Engine_1 = __webpack_require__(331);
const debug = debug_1.default('engine');
const readFile = util_1.promisify(fs.readFile);
/**
 * Node.js based wrapper to run the Prisma binary
 */
class NodeEngine extends Engine_1.Engine {
    constructor(_a) {
        var { prismaConfig, datamodelJson, prismaYmlPath, datamodel, schemaInferrerPath, prismaPath } = _a, args = __rest(_a, ["prismaConfig", "datamodelJson", "prismaYmlPath", "datamodel", "schemaInferrerPath", "prismaPath"]);
        super();
        /**
         * exiting is used to tell the .on('exit') hook, if the exit came from our script.
         * As soon as the Prisma binary returns a correct return code (like 1 or 0), we don't need this anymore
         */
        this.exiting = false;
        this.managementApiEnabled = false;
        this.errorLogs = '';
        /**
         * If Prisma runs, stop it
         */
        this.stop = () => {
            if (this.child) {
                debug(`Stopping Prisma engine`);
                this.exiting = true;
                this.child.kill();
                delete this.child;
            }
        };
        this.prismaYmlPath = prismaYmlPath;
        this.prismaConfig = prismaConfig;
        this.cwd = prismaYmlPath ? path.dirname(this.prismaYmlPath) : process.cwd();
        this.debug = args.debug || false;
        this.datamodelJson = datamodelJson;
        this.datamodel = datamodel;
        this.schemaInferrerPath = schemaInferrerPath || NodeEngine.defaultSchemaInferrerPath;
        this.prismaPath = prismaPath || NodeEngine.defaultPrismaPath;
        if (this.debug) {
            debug_1.default.enable('engine');
        }
        this.startPromise = this.start();
    }
    /**
     * Resolve the prisma.yml
     */
    async getPrismaYml(ymlPath) {
        return await readFile(ymlPath, 'utf-8');
    }
    /**
     * Starts the engine, returns the url that it runs on
     */
    start() {
        if (this.startPromise) {
            return this.startPromise;
        }
        return new Promise(async (resolve, reject) => {
            this.port = await this.getFreePort();
            this.prismaConfig = this.prismaConfig || (await this.getPrismaYml(this.prismaYmlPath));
            const PRISMA_CONFIG = this.generatePrismaConfig();
            const schemaEnv = {};
            if (!this.datamodelJson) {
                this.datamodelJson = await getInternalDatamodelJson_1.getInternalDatamodelJson(this.datamodel, this.schemaInferrerPath);
            }
            debug(`Starting binary at ${this.prismaPath}`);
            const env = Object.assign({ PRISMA_CONFIG, PRISMA_SDL: this.datamodel, PRISMA_DML: '', SERVER_ROOT: process.cwd(), PRISMA_INTERNAL_DATA_MODEL_JSON: this.datamodelJson }, schemaEnv);
            debug(env);
            this.child = child_process_1.spawn(this.prismaPath, [], {
                env,
                detached: false,
                cwd: this.cwd,
            });
            this.child.stderr.on('data', d => {
                const str = d.toString();
                this.errorLogs += str;
                debug(str);
            });
            this.child.stdout.on('data', d => {
                debug(d.toString());
            });
            this.child.on('error', e => {
                this.errorLogs += e.toString();
                debug(e);
                reject(e);
            });
            this.child.on('exit', (code, e) => {
                if (code !== 0 && !this.exiting) {
                    const debugString = this.debug
                        ? ''
                        : 'Please enable "debug": true in the Engine constructor to get more insights.';
                    throw new Error(`Child exited with code ${code}${debugString}${e}`);
                }
            });
            // Make sure we kill Rust when this process is being killed
            process.once('SIGTERM', this.stop);
            process.once('SIGINT', this.stop);
            process.once('uncaughtException', this.stop);
            process.once('unhandledRejection', this.stop);
            await this.engineReady();
            const url = `http://localhost:${this.port}`;
            this.url = url;
            resolve();
        });
    }
    /**
     * Use the port 0 trick to get a new port
     */
    getFreePort() {
        return new Promise((resolve, reject) => {
            const server = net.createServer(s => s.end(''));
            server.listen(0, () => {
                const address = server.address();
                const port = typeof address === 'string' ? parseInt(address.split(':').slice(-1)[0], 10) : address.port;
                server.close(e => {
                    if (e) {
                        reject(e);
                    }
                    resolve(port);
                });
            });
        });
    }
    /**
     * Replace the port in the Prisma Config
     */
    generatePrismaConfig() {
        return `port: ${this.port}\n${this.trimPort(this.prismaConfig)}`;
    }
    /**
     * Make sure that our internal port is not conflicting with the prisma.yml's port
     * @param str config
     */
    trimPort(str) {
        return str
            .split('\n')
            .filter(l => !l.startsWith('port:'))
            .join('\n');
    }
    // TODO: Replace it with a simple tcp connection
    async engineReady() {
        let tries = 0;
        while (true) {
            try {
                const response = await cross_fetch_1.default(`http://localhost:${this.port}/datamodel`);
                await new Promise(r => setTimeout(r, 50)); // TODO: Try out lower intervals here, but we also don't want to spam it too much.
                if (response.ok) {
                    debug(`Ready after try number ${tries}`);
                    return;
                }
            }
            catch (e) {
                debug(e.message);
                if (tries >= 100) {
                    throw e;
                }
            }
            finally {
                tries++;
            }
        }
    }
    async request(query) {
        if (!this.url) {
            await this.startPromise; // allows lazily connecting the client to Rust and Rust to the Datasource
        }
        return cross_fetch_1.default(this.url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ query, variables: {}, operationName: '' }),
        })
            .then(response => {
            if (!response.ok) {
                return response.text().then(body => {
                    const { status, statusText } = response;
                    this.handleErrors({
                        errors: {
                            status,
                            statusText,
                            body,
                        },
                        query,
                    });
                });
            }
            else {
                return response.json().then(result => {
                    const { data } = result;
                    const errors = result.error || result.errors;
                    if (errors) {
                        return this.handleErrors({
                            errors,
                            query,
                        });
                    }
                    return data;
                });
            }
        })
            .catch(errors => {
            if (!(errors instanceof Engine_1.PhotonError)) {
                return this.handleErrors({ errors, query });
            }
            else {
                throw errors;
            }
        });
    }
    handleErrors({ errors, query }) {
        const stringified = errors ? JSON.stringify(errors, null, 2) : null;
        const message = stringified.length > 0 ? stringified : `Error in prisma.\$\{rootField || 'query'}`;
        const isPanicked = this.errorLogs.includes('panicked');
        if (isPanicked) {
            this.stop();
        }
        throw new Engine_1.PhotonError(message, query, errors, this.errorLogs, isPanicked);
    }
}
NodeEngine.defaultSchemaInferrerPath = path.join(__dirname + '/dist', '../schema-inferrer-bin');
NodeEngine.defaultPrismaPath = path.join(__dirname + '/dist', '../prisma');
exports.NodeEngine = NodeEngine;
//# sourceMappingURL=NodeEngine.js.map

/***/ }),

/***/ 171:
/***/ (function(module, __unusedexports, __webpack_require__) {


/**
 * This is the common logic for both the Node.js and web browser
 * implementations of `debug()`.
 */

function setup(env) {
	createDebug.debug = createDebug;
	createDebug.default = createDebug;
	createDebug.coerce = coerce;
	createDebug.disable = disable;
	createDebug.enable = enable;
	createDebug.enabled = enabled;
	createDebug.humanize = __webpack_require__(441);

	Object.keys(env).forEach(key => {
		createDebug[key] = env[key];
	});

	/**
	* Active `debug` instances.
	*/
	createDebug.instances = [];

	/**
	* The currently active debug mode names, and names to skip.
	*/

	createDebug.names = [];
	createDebug.skips = [];

	/**
	* Map of special "%n" handling functions, for the debug "format" argument.
	*
	* Valid key names are a single, lower or upper-case letter, i.e. "n" and "N".
	*/
	createDebug.formatters = {};

	/**
	* Selects a color for a debug namespace
	* @param {String} namespace The namespace string for the for the debug instance to be colored
	* @return {Number|String} An ANSI color code for the given namespace
	* @api private
	*/
	function selectColor(namespace) {
		let hash = 0;

		for (let i = 0; i < namespace.length; i++) {
			hash = ((hash << 5) - hash) + namespace.charCodeAt(i);
			hash |= 0; // Convert to 32bit integer
		}

		return createDebug.colors[Math.abs(hash) % createDebug.colors.length];
	}
	createDebug.selectColor = selectColor;

	/**
	* Create a debugger with the given `namespace`.
	*
	* @param {String} namespace
	* @return {Function}
	* @api public
	*/
	function createDebug(namespace) {
		let prevTime;

		function debug(...args) {
			// Disabled?
			if (!debug.enabled) {
				return;
			}

			const self = debug;

			// Set `diff` timestamp
			const curr = Number(new Date());
			const ms = curr - (prevTime || curr);
			self.diff = ms;
			self.prev = prevTime;
			self.curr = curr;
			prevTime = curr;

			args[0] = createDebug.coerce(args[0]);

			if (typeof args[0] !== 'string') {
				// Anything else let's inspect with %O
				args.unshift('%O');
			}

			// Apply any `formatters` transformations
			let index = 0;
			args[0] = args[0].replace(/%([a-zA-Z%])/g, (match, format) => {
				// If we encounter an escaped % then don't increase the array index
				if (match === '%%') {
					return match;
				}
				index++;
				const formatter = createDebug.formatters[format];
				if (typeof formatter === 'function') {
					const val = args[index];
					match = formatter.call(self, val);

					// Now we need to remove `args[index]` since it's inlined in the `format`
					args.splice(index, 1);
					index--;
				}
				return match;
			});

			// Apply env-specific formatting (colors, etc.)
			createDebug.formatArgs.call(self, args);

			const logFn = self.log || createDebug.log;
			logFn.apply(self, args);
		}

		debug.namespace = namespace;
		debug.enabled = createDebug.enabled(namespace);
		debug.useColors = createDebug.useColors();
		debug.color = selectColor(namespace);
		debug.destroy = destroy;
		debug.extend = extend;
		// Debug.formatArgs = formatArgs;
		// debug.rawLog = rawLog;

		// env-specific initialization logic for debug instances
		if (typeof createDebug.init === 'function') {
			createDebug.init(debug);
		}

		createDebug.instances.push(debug);

		return debug;
	}

	function destroy() {
		const index = createDebug.instances.indexOf(this);
		if (index !== -1) {
			createDebug.instances.splice(index, 1);
			return true;
		}
		return false;
	}

	function extend(namespace, delimiter) {
		const newDebug = createDebug(this.namespace + (typeof delimiter === 'undefined' ? ':' : delimiter) + namespace);
		newDebug.log = this.log;
		return newDebug;
	}

	/**
	* Enables a debug mode by namespaces. This can include modes
	* separated by a colon and wildcards.
	*
	* @param {String} namespaces
	* @api public
	*/
	function enable(namespaces) {
		createDebug.save(namespaces);

		createDebug.names = [];
		createDebug.skips = [];

		let i;
		const split = (typeof namespaces === 'string' ? namespaces : '').split(/[\s,]+/);
		const len = split.length;

		for (i = 0; i < len; i++) {
			if (!split[i]) {
				// ignore empty strings
				continue;
			}

			namespaces = split[i].replace(/\*/g, '.*?');

			if (namespaces[0] === '-') {
				createDebug.skips.push(new RegExp('^' + namespaces.substr(1) + '$'));
			} else {
				createDebug.names.push(new RegExp('^' + namespaces + '$'));
			}
		}

		for (i = 0; i < createDebug.instances.length; i++) {
			const instance = createDebug.instances[i];
			instance.enabled = createDebug.enabled(instance.namespace);
		}
	}

	/**
	* Disable debug output.
	*
	* @return {String} namespaces
	* @api public
	*/
	function disable() {
		const namespaces = [
			...createDebug.names.map(toNamespace),
			...createDebug.skips.map(toNamespace).map(namespace => '-' + namespace)
		].join(',');
		createDebug.enable('');
		return namespaces;
	}

	/**
	* Returns true if the given mode name is enabled, false otherwise.
	*
	* @param {String} name
	* @return {Boolean}
	* @api public
	*/
	function enabled(name) {
		if (name[name.length - 1] === '*') {
			return true;
		}

		let i;
		let len;

		for (i = 0, len = createDebug.skips.length; i < len; i++) {
			if (createDebug.skips[i].test(name)) {
				return false;
			}
		}

		for (i = 0, len = createDebug.names.length; i < len; i++) {
			if (createDebug.names[i].test(name)) {
				return true;
			}
		}

		return false;
	}

	/**
	* Convert regexp to namespace
	*
	* @param {RegExp} regxep
	* @return {String} namespace
	* @api private
	*/
	function toNamespace(regexp) {
		return regexp.toString()
			.substring(2, regexp.toString().length - 2)
			.replace(/\.\*\?$/, '*');
	}

	/**
	* Coerce `val`.
	*
	* @param {Mixed} val
	* @return {Mixed}
	* @api private
	*/
	function coerce(val) {
		if (val instanceof Error) {
			return val.stack || val.message;
		}
		return val;
	}

	createDebug.enable(createDebug.load());

	return createDebug;
}

module.exports = setup;


/***/ }),

/***/ 193:
/***/ (function(module) {

module.exports = require("net");

/***/ }),

/***/ 197:
/***/ (function(module, __unusedexports, __webpack_require__) {

/**
 * Detect Electron renderer / nwjs process, which is node, but we should
 * treat as a browser.
 */

if (typeof process === 'undefined' || process.type === 'renderer' || process.browser === true || process.__nwjs) {
	module.exports = __webpack_require__(583);
} else {
	module.exports = __webpack_require__(894);
}


/***/ }),

/***/ 204:
/***/ (function(module) {

module.exports = require("child_process");

/***/ }),

/***/ 230:
/***/ (function(module) {

module.exports = require("https");

/***/ }),

/***/ 247:
/***/ (function(module, __unusedexports, __webpack_require__) {


/**
 * This is the common logic for both the Node.js and web browser
 * implementations of `debug()`.
 */

function setup(env) {
	createDebug.debug = createDebug;
	createDebug.default = createDebug;
	createDebug.coerce = coerce;
	createDebug.disable = disable;
	createDebug.enable = enable;
	createDebug.enabled = enabled;
	createDebug.humanize = __webpack_require__(86);

	Object.keys(env).forEach(key => {
		createDebug[key] = env[key];
	});

	/**
	* Active `debug` instances.
	*/
	createDebug.instances = [];

	/**
	* The currently active debug mode names, and names to skip.
	*/

	createDebug.names = [];
	createDebug.skips = [];

	/**
	* Map of special "%n" handling functions, for the debug "format" argument.
	*
	* Valid key names are a single, lower or upper-case letter, i.e. "n" and "N".
	*/
	createDebug.formatters = {};

	/**
	* Selects a color for a debug namespace
	* @param {String} namespace The namespace string for the for the debug instance to be colored
	* @return {Number|String} An ANSI color code for the given namespace
	* @api private
	*/
	function selectColor(namespace) {
		let hash = 0;

		for (let i = 0; i < namespace.length; i++) {
			hash = ((hash << 5) - hash) + namespace.charCodeAt(i);
			hash |= 0; // Convert to 32bit integer
		}

		return createDebug.colors[Math.abs(hash) % createDebug.colors.length];
	}
	createDebug.selectColor = selectColor;

	/**
	* Create a debugger with the given `namespace`.
	*
	* @param {String} namespace
	* @return {Function}
	* @api public
	*/
	function createDebug(namespace) {
		let prevTime;

		function debug(...args) {
			// Disabled?
			if (!debug.enabled) {
				return;
			}

			const self = debug;

			// Set `diff` timestamp
			const curr = Number(new Date());
			const ms = curr - (prevTime || curr);
			self.diff = ms;
			self.prev = prevTime;
			self.curr = curr;
			prevTime = curr;

			args[0] = createDebug.coerce(args[0]);

			if (typeof args[0] !== 'string') {
				// Anything else let's inspect with %O
				args.unshift('%O');
			}

			// Apply any `formatters` transformations
			let index = 0;
			args[0] = args[0].replace(/%([a-zA-Z%])/g, (match, format) => {
				// If we encounter an escaped % then don't increase the array index
				if (match === '%%') {
					return match;
				}
				index++;
				const formatter = createDebug.formatters[format];
				if (typeof formatter === 'function') {
					const val = args[index];
					match = formatter.call(self, val);

					// Now we need to remove `args[index]` since it's inlined in the `format`
					args.splice(index, 1);
					index--;
				}
				return match;
			});

			// Apply env-specific formatting (colors, etc.)
			createDebug.formatArgs.call(self, args);

			const logFn = self.log || createDebug.log;
			logFn.apply(self, args);
		}

		debug.namespace = namespace;
		debug.enabled = createDebug.enabled(namespace);
		debug.useColors = createDebug.useColors();
		debug.color = selectColor(namespace);
		debug.destroy = destroy;
		debug.extend = extend;
		// Debug.formatArgs = formatArgs;
		// debug.rawLog = rawLog;

		// env-specific initialization logic for debug instances
		if (typeof createDebug.init === 'function') {
			createDebug.init(debug);
		}

		createDebug.instances.push(debug);

		return debug;
	}

	function destroy() {
		const index = createDebug.instances.indexOf(this);
		if (index !== -1) {
			createDebug.instances.splice(index, 1);
			return true;
		}
		return false;
	}

	function extend(namespace, delimiter) {
		const newDebug = createDebug(this.namespace + (typeof delimiter === 'undefined' ? ':' : delimiter) + namespace);
		newDebug.log = this.log;
		return newDebug;
	}

	/**
	* Enables a debug mode by namespaces. This can include modes
	* separated by a colon and wildcards.
	*
	* @param {String} namespaces
	* @api public
	*/
	function enable(namespaces) {
		createDebug.save(namespaces);

		createDebug.names = [];
		createDebug.skips = [];

		let i;
		const split = (typeof namespaces === 'string' ? namespaces : '').split(/[\s,]+/);
		const len = split.length;

		for (i = 0; i < len; i++) {
			if (!split[i]) {
				// ignore empty strings
				continue;
			}

			namespaces = split[i].replace(/\*/g, '.*?');

			if (namespaces[0] === '-') {
				createDebug.skips.push(new RegExp('^' + namespaces.substr(1) + '$'));
			} else {
				createDebug.names.push(new RegExp('^' + namespaces + '$'));
			}
		}

		for (i = 0; i < createDebug.instances.length; i++) {
			const instance = createDebug.instances[i];
			instance.enabled = createDebug.enabled(instance.namespace);
		}
	}

	/**
	* Disable debug output.
	*
	* @return {String} namespaces
	* @api public
	*/
	function disable() {
		const namespaces = [
			...createDebug.names.map(toNamespace),
			...createDebug.skips.map(toNamespace).map(namespace => '-' + namespace)
		].join(',');
		createDebug.enable('');
		return namespaces;
	}

	/**
	* Returns true if the given mode name is enabled, false otherwise.
	*
	* @param {String} name
	* @return {Boolean}
	* @api public
	*/
	function enabled(name) {
		if (name[name.length - 1] === '*') {
			return true;
		}

		let i;
		let len;

		for (i = 0, len = createDebug.skips.length; i < len; i++) {
			if (createDebug.skips[i].test(name)) {
				return false;
			}
		}

		for (i = 0, len = createDebug.names.length; i < len; i++) {
			if (createDebug.names[i].test(name)) {
				return true;
			}
		}

		return false;
	}

	/**
	* Convert regexp to namespace
	*
	* @param {RegExp} regxep
	* @return {String} namespace
	* @api private
	*/
	function toNamespace(regexp) {
		return regexp.toString()
			.substring(2, regexp.toString().length - 2)
			.replace(/\.\*\?$/, '*');
	}

	/**
	* Coerce `val`.
	*
	* @param {Mixed} val
	* @return {Mixed}
	* @api private
	*/
	function coerce(val) {
		if (val instanceof Error) {
			return val.stack || val.message;
		}
		return val;
	}

	createDebug.enable(createDebug.load());

	return createDebug;
}

module.exports = setup;


/***/ }),

/***/ 267:
/***/ (function(module) {

module.exports = require("zlib");

/***/ }),

/***/ 269:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";

const os = __webpack_require__(431);
const hasFlag = __webpack_require__(834);

const env = process.env;

let forceColor;
if (hasFlag('no-color') ||
	hasFlag('no-colors') ||
	hasFlag('color=false')) {
	forceColor = false;
} else if (hasFlag('color') ||
	hasFlag('colors') ||
	hasFlag('color=true') ||
	hasFlag('color=always')) {
	forceColor = true;
}
if ('FORCE_COLOR' in env) {
	forceColor = env.FORCE_COLOR.length === 0 || parseInt(env.FORCE_COLOR, 10) !== 0;
}

function translateLevel(level) {
	if (level === 0) {
		return false;
	}

	return {
		level,
		hasBasic: true,
		has256: level >= 2,
		has16m: level >= 3
	};
}

function supportsColor(stream) {
	if (forceColor === false) {
		return 0;
	}

	if (hasFlag('color=16m') ||
		hasFlag('color=full') ||
		hasFlag('color=truecolor')) {
		return 3;
	}

	if (hasFlag('color=256')) {
		return 2;
	}

	if (stream && !stream.isTTY && forceColor !== true) {
		return 0;
	}

	const min = forceColor ? 1 : 0;

	if (process.platform === 'win32') {
		// Node.js 7.5.0 is the first version of Node.js to include a patch to
		// libuv that enables 256 color output on Windows. Anything earlier and it
		// won't work. However, here we target Node.js 8 at minimum as it is an LTS
		// release, and Node.js 7 is not. Windows 10 build 10586 is the first Windows
		// release that supports 256 colors. Windows 10 build 14931 is the first release
		// that supports 16m/TrueColor.
		const osRelease = os.release().split('.');
		if (
			Number(process.versions.node.split('.')[0]) >= 8 &&
			Number(osRelease[0]) >= 10 &&
			Number(osRelease[2]) >= 10586
		) {
			return Number(osRelease[2]) >= 14931 ? 3 : 2;
		}

		return 1;
	}

	if ('CI' in env) {
		if (['TRAVIS', 'CIRCLECI', 'APPVEYOR', 'GITLAB_CI'].some(sign => sign in env) || env.CI_NAME === 'codeship') {
			return 1;
		}

		return min;
	}

	if ('TEAMCITY_VERSION' in env) {
		return /^(9\.(0*[1-9]\d*)\.|\d{2,}\.)/.test(env.TEAMCITY_VERSION) ? 1 : 0;
	}

	if (env.COLORTERM === 'truecolor') {
		return 3;
	}

	if ('TERM_PROGRAM' in env) {
		const version = parseInt((env.TERM_PROGRAM_VERSION || '').split('.')[0], 10);

		switch (env.TERM_PROGRAM) {
			case 'iTerm.app':
				return version >= 3 ? 3 : 2;
			case 'Apple_Terminal':
				return 2;
			// No default
		}
	}

	if (/-256(color)?$/i.test(env.TERM)) {
		return 2;
	}

	if (/^screen|^xterm|^vt100|^vt220|^rxvt|color|ansi|cygwin|linux/i.test(env.TERM)) {
		return 1;
	}

	if ('COLORTERM' in env) {
		return 1;
	}

	if (env.TERM === 'dumb') {
		return min;
	}

	return min;
}

function getSupportLevel(stream) {
	const level = supportsColor(stream);
	return translateLevel(level);
}

module.exports = {
	supportsColor: getSupportLevel,
	stdout: getSupportLevel(process.stdout),
	stderr: getSupportLevel(process.stderr)
};


/***/ }),

/***/ 275:
/***/ (function(module, __unusedexports, __webpack_require__) {

var conversions = __webpack_require__(752);
var route = __webpack_require__(622);

var convert = {};

var models = Object.keys(conversions);

function wrapRaw(fn) {
	var wrappedFn = function (args) {
		if (args === undefined || args === null) {
			return args;
		}

		if (arguments.length > 1) {
			args = Array.prototype.slice.call(arguments);
		}

		return fn(args);
	};

	// preserve .conversion property if there is one
	if ('conversion' in fn) {
		wrappedFn.conversion = fn.conversion;
	}

	return wrappedFn;
}

function wrapRounded(fn) {
	var wrappedFn = function (args) {
		if (args === undefined || args === null) {
			return args;
		}

		if (arguments.length > 1) {
			args = Array.prototype.slice.call(arguments);
		}

		var result = fn(args);

		// we're assuming the result is an array here.
		// see notice in conversions.js; don't use box types
		// in conversion functions.
		if (typeof result === 'object') {
			for (var len = result.length, i = 0; i < len; i++) {
				result[i] = Math.round(result[i]);
			}
		}

		return result;
	};

	// preserve .conversion property if there is one
	if ('conversion' in fn) {
		wrappedFn.conversion = fn.conversion;
	}

	return wrappedFn;
}

models.forEach(function (fromModel) {
	convert[fromModel] = {};

	Object.defineProperty(convert[fromModel], 'channels', {value: conversions[fromModel].channels});
	Object.defineProperty(convert[fromModel], 'labels', {value: conversions[fromModel].labels});

	var routes = route(fromModel);
	var routeModels = Object.keys(routes);

	routeModels.forEach(function (toModel) {
		var fn = routes[toModel];

		convert[fromModel][toModel] = wrapRounded(fn);
		convert[fromModel][toModel].raw = wrapRaw(fn);
	});
});

module.exports = convert;


/***/ }),

/***/ 321:
/***/ (function(module) {

"use strict";


module.exports = input => Object.prototype.toString.call(input) === '[object RegExp]';


/***/ }),

/***/ 328:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const path_1 = __importDefault(__webpack_require__(589));
const child_process_1 = __webpack_require__(204);
const byline_1 = __importDefault(__webpack_require__(868));
function getInternalDatamodelJson(datamodel, schemaInferrerPath = path_1.default.join(__dirname + '/dist', '../schema-inferrer-bin')) {
    return new Promise((resolve, reject) => {
        const proc = child_process_1.spawn(schemaInferrerPath, { stdio: ['pipe', 'pipe', process.stderr] });
        proc.on('error', function (err) {
            console.error('[schema-inferrer-bin] error: %s', err);
            reject(err);
        });
        proc.on('exit', function (code, signal) {
            if (code !== 0) {
                console.error('[schema-inferrer-bin] exit: code=%s signal=%s', code, signal);
            }
            reject();
        });
        const out = byline_1.default(proc.stdout);
        out.on('data', line => {
            const result = JSON.parse(line);
            const resultB64 = Buffer.from(JSON.stringify(result)).toString('base64');
            resolve(resultB64);
        });
        const cut = datamodel.replace(/\n/g, ' ');
        proc.stdin.write(JSON.stringify({ dataModel: cut }) + '\n');
    });
}
exports.getInternalDatamodelJson = getInternalDatamodelJson;
//# sourceMappingURL=getInternalDatamodelJson.js.map

/***/ }),

/***/ 331:
/***/ (function(__unusedmodule, exports) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
class PhotonError extends Error {
    constructor(message, query, error, logs, isPanicked) {
        super(message);
        this.message = message;
        this.query = query;
        this.error = error;
        this.logs = logs;
        this.isPanicked = isPanicked;
    }
}
exports.PhotonError = PhotonError;
/**
 * Engine Base Class used by Browser and Node.js
 */
class Engine {
}
exports.Engine = Engine;
//# sourceMappingURL=Engine.js.map

/***/ }),

/***/ 421:
/***/ (function(module) {

module.exports = require("http");

/***/ }),

/***/ 431:
/***/ (function(module) {

module.exports = require("os");

/***/ }),

/***/ 441:
/***/ (function(module) {

/**
 * Helpers.
 */

var s = 1000;
var m = s * 60;
var h = m * 60;
var d = h * 24;
var w = d * 7;
var y = d * 365.25;

/**
 * Parse or format the given `val`.
 *
 * Options:
 *
 *  - `long` verbose formatting [false]
 *
 * @param {String|Number} val
 * @param {Object} [options]
 * @throws {Error} throw an error if val is not a non-empty string or a number
 * @return {String|Number}
 * @api public
 */

module.exports = function(val, options) {
  options = options || {};
  var type = typeof val;
  if (type === 'string' && val.length > 0) {
    return parse(val);
  } else if (type === 'number' && isNaN(val) === false) {
    return options.long ? fmtLong(val) : fmtShort(val);
  }
  throw new Error(
    'val is not a non-empty string or a valid number. val=' +
      JSON.stringify(val)
  );
};

/**
 * Parse the given `str` and return milliseconds.
 *
 * @param {String} str
 * @return {Number}
 * @api private
 */

function parse(str) {
  str = String(str);
  if (str.length > 100) {
    return;
  }
  var match = /^((?:\d+)?\-?\d?\.?\d+) *(milliseconds?|msecs?|ms|seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h|days?|d|weeks?|w|years?|yrs?|y)?$/i.exec(
    str
  );
  if (!match) {
    return;
  }
  var n = parseFloat(match[1]);
  var type = (match[2] || 'ms').toLowerCase();
  switch (type) {
    case 'years':
    case 'year':
    case 'yrs':
    case 'yr':
    case 'y':
      return n * y;
    case 'weeks':
    case 'week':
    case 'w':
      return n * w;
    case 'days':
    case 'day':
    case 'd':
      return n * d;
    case 'hours':
    case 'hour':
    case 'hrs':
    case 'hr':
    case 'h':
      return n * h;
    case 'minutes':
    case 'minute':
    case 'mins':
    case 'min':
    case 'm':
      return n * m;
    case 'seconds':
    case 'second':
    case 'secs':
    case 'sec':
    case 's':
      return n * s;
    case 'milliseconds':
    case 'millisecond':
    case 'msecs':
    case 'msec':
    case 'ms':
      return n;
    default:
      return undefined;
  }
}

/**
 * Short format for `ms`.
 *
 * @param {Number} ms
 * @return {String}
 * @api private
 */

function fmtShort(ms) {
  var msAbs = Math.abs(ms);
  if (msAbs >= d) {
    return Math.round(ms / d) + 'd';
  }
  if (msAbs >= h) {
    return Math.round(ms / h) + 'h';
  }
  if (msAbs >= m) {
    return Math.round(ms / m) + 'm';
  }
  if (msAbs >= s) {
    return Math.round(ms / s) + 's';
  }
  return ms + 'ms';
}

/**
 * Long format for `ms`.
 *
 * @param {Number} ms
 * @return {String}
 * @api private
 */

function fmtLong(ms) {
  var msAbs = Math.abs(ms);
  if (msAbs >= d) {
    return plural(ms, msAbs, d, 'day');
  }
  if (msAbs >= h) {
    return plural(ms, msAbs, h, 'hour');
  }
  if (msAbs >= m) {
    return plural(ms, msAbs, m, 'minute');
  }
  if (msAbs >= s) {
    return plural(ms, msAbs, s, 'second');
  }
  return ms + ' ms';
}

/**
 * Pluralization helper.
 */

function plural(ms, msAbs, n, name) {
  var isPlural = msAbs >= n * 1.5;
  return Math.round(ms / n) + ' ' + name + (isPlural ? 's' : '');
}


/***/ }),

/***/ 446:
/***/ (function(module) {

"use strict";


module.exports = options => {
	options = Object.assign({
		onlyFirst: false
	}, options);

	const pattern = [
		'[\\u001B\\u009B][[\\]()#;?]*(?:(?:(?:[a-zA-Z\\d]*(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]*)*)?\\u0007)',
		'(?:(?:\\d{1,4}(?:;\\d{0,4})*)?[\\dA-PR-TZcf-ntqry=><~]))'
	].join('|');

	return new RegExp(pattern, options.onlyFirst ? undefined : 'g');
};


/***/ }),

/***/ 457:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const chalk_1 = __importDefault(__webpack_require__(936));
const indent_string_1 = __importDefault(__webpack_require__(484));
const common_1 = __webpack_require__(957);
const deep_extend_1 = __webpack_require__(494);
const deep_set_1 = __webpack_require__(953);
const filterObject_1 = __webpack_require__(659);
const omit_1 = __webpack_require__(772);
const printJsonErrors_1 = __webpack_require__(50);
const stringifyObject_1 = __importDefault(__webpack_require__(763));
const visit_1 = __webpack_require__(805);
const tab = 2;
class Document {
    constructor(type, children) {
        this.type = type;
        this.children = children;
        this.printFieldError = ({ error }) => {
            if (error.type === 'invalidFieldName') {
                let str = `Unknown field ${chalk_1.default.redBright(`\`${error.providedName}\``)} on model ${chalk_1.default.bold.white(error.modelName)}.`;
                if (error.didYouMean) {
                    str += ` Did you mean ${chalk_1.default.greenBright(`\`${error.didYouMean}\``)}?`;
                }
                return str;
            }
            if (error.type === 'invalidFieldType') {
                const str = `Invalid value ${chalk_1.default.redBright(`${stringifyObject_1.default(error.providedValue)}`)} of type ${chalk_1.default.redBright(common_1.getGraphQLType(error.providedValue, undefined))} for field ${chalk_1.default.bold(`${error.fieldName}`)} on model ${chalk_1.default.bold.white(error.modelName)}. Expected either ${chalk_1.default.greenBright('true')} or ${chalk_1.default.greenBright('false')}.`;
                return str;
            }
        };
        this.printArgError = ({ error, path }) => {
            if (error.type === 'invalidName') {
                let str = `Unknown arg ${chalk_1.default.redBright(`\`${error.providedName}\``)} in ${chalk_1.default.bold(path.join('.'))}. for type ${chalk_1.default.bold(error.outputType ? error.outputType.name : common_1.getInputTypeName(error.originalType))}.`;
                if (error.didYouMeanField) {
                    str += `\n→ Did you forget to wrap it with \`${chalk_1.default.greenBright('select')}\`? ${chalk_1.default.dim('e.g. ' + chalk_1.default.greenBright(`{ select: { ${error.providedName}: ${error.providedValue} } }`))}`;
                }
                else if (error.didYouMeanArg) {
                    str += ` Did you mean \`${chalk_1.default.greenBright(error.didYouMeanArg)}\`?`;
                    str += ` ${chalk_1.default.dim('Available args:\n')}` + common_1.stringifyInputType(error.originalType, true);
                }
                else {
                    if (error.originalType.args.length === 0) {
                        str += ` The field ${chalk_1.default.bold(error.originalType.name)} has no arguments.`;
                    }
                    else {
                        str += ` Available args:\n` + common_1.stringifyInputType(error.originalType, true);
                    }
                }
                return str;
            }
            if (error.type === 'invalidType') {
                let valueStr = stringifyObject_1.default(error.providedValue, { indent: '  ' });
                const multilineValue = valueStr.split('\n').length > 1;
                if (multilineValue) {
                    valueStr = `\n${valueStr}\n`;
                }
                // TODO: we don't yet support enums in a union with a non enum. This is mostly due to not implemented error handling
                // at this code part.
                if (error.requiredType.isEnum) {
                    return `Argument ${chalk_1.default.bold(error.argName)}: Provided value ${chalk_1.default.redBright(valueStr)}${multilineValue ? '' : ' '}of type ${chalk_1.default.redBright(common_1.getGraphQLType(error.providedValue))} on ${chalk_1.default.bold(`photon.${this.children[0].name}`)} is not a ${chalk_1.default.greenBright(common_1.wrapWithList(common_1.stringifyGraphQLType(error.requiredType.bestFittingType), error.requiredType.isList))}.
→ Possible values: ${error.requiredType.bestFittingType.values
                        .map(v => chalk_1.default.greenBright(`${common_1.stringifyGraphQLType(error.requiredType.bestFittingType)}.${v}`))
                        .join(', ')}`;
                }
                let typeStr = '.';
                if (isInputArgType(error.requiredType.bestFittingType)) {
                    typeStr = ':\n' + common_1.stringifyInputType(error.requiredType.bestFittingType);
                }
                let expected = `${error.requiredType.types
                    .map(t => chalk_1.default.greenBright(common_1.wrapWithList(common_1.stringifyGraphQLType(t), error.requiredType.isList)))
                    .join(' or ')}${typeStr}`;
                const inputType = (error.requiredType.types.length === 2 &&
                    error.requiredType.types.find(t => isInputArgType(t))) || null;
                if (inputType) {
                    expected += `\n` + common_1.stringifyInputType(inputType, true);
                }
                return `Argument ${chalk_1.default.bold(error.argName)}: Got invalid value ${chalk_1.default.redBright(valueStr)}${multilineValue ? '' : ' '}on ${chalk_1.default.bold(`photon.${this.children[0].name}`)}. Provided ${chalk_1.default.redBright(common_1.getGraphQLType(error.providedValue))}, expected ${expected}`;
            }
            if (error.type === 'missingArg') {
                return `Argument ${chalk_1.default.greenBright(error.missingName)} for ${chalk_1.default.bold(`photon.${path.join('.')}`)} is missing. You can see in ${chalk_1.default.greenBright('green')} what you need to add.`;
            }
            if (error.type === 'atLeastOne') {
                return `Argument ${chalk_1.default.bold(path.join('.'))} of type ${chalk_1.default.bold(error.inputType.name)} needs ${chalk_1.default.greenBright('at least one')} argument. Available args are listed in ${chalk_1.default.dim.green('green')}.`;
            }
            if (error.type === 'atMostOne') {
                return `Argument ${chalk_1.default.bold(path.join('.'))} of type ${chalk_1.default.bold(error.inputType.name)} needs ${chalk_1.default.greenBright('exactly one')} argument, but you provided ${error.providedKeys
                    .map(key => chalk_1.default.redBright(key))
                    .join(' and ')}. Please choose one. ${chalk_1.default.dim('Available args:')} \n${common_1.stringifyInputType(error.inputType, true)}`;
            }
        };
        this.type = type;
        this.children = children;
    }
    toString() {
        return `${this.type} {
${indent_string_1.default(this.children.map(String).join('\n'), tab)}
}`;
    }
    validate(select, isTopLevelQuery = false, originalMethod) {
        const invalidChildren = this.children.filter(child => child.hasInvalidChild || child.hasInvalidArg);
        if (invalidChildren.length === 0) {
            return;
        }
        const fieldErrors = [];
        const argErrors = [];
        for (const child of invalidChildren) {
            const errors = child.collectErrors();
            fieldErrors.push(...errors.fieldErrors.map(e => (Object.assign({}, e, { path: isTopLevelQuery ? e.path : e.path.slice(1) }))));
            argErrors.push(...errors.argErrors.map(e => (Object.assign({}, e, { path: isTopLevelQuery ? e.path : e.path.slice(1) }))));
        }
        const topLevelQueryName = this.children[0].name;
        const queryName = isTopLevelQuery ? this.type : topLevelQueryName;
        const keyPaths = [];
        const valuePaths = [];
        const missingItems = [];
        for (const fieldError of fieldErrors) {
            const path = this.normalizePath(fieldError.path, select).join('.');
            if (fieldError.error.type === 'invalidFieldName') {
                keyPaths.push(path);
            }
            else {
                valuePaths.push(path);
            }
        }
        // an arg error can either be an invalid key or invalid value
        for (const argError of argErrors) {
            const path = this.normalizePath(argError.path, select).join('.');
            if (argError.error.type === 'invalidName') {
                keyPaths.push(path);
            }
            else if (argError.error.type !== 'missingArg' && argError.error.type !== 'atLeastOne') {
                valuePaths.push(path);
            }
            else if (argError.error.type === 'missingArg') {
                const type = argError.error.missingType.length === 1
                    ? argError.error.missingType[0]
                    : argError.error.missingType.map(common_1.getInputTypeName).join(' | ');
                missingItems.push({
                    path,
                    type: common_1.inputTypeToJson(type, true, path.split('where.').length === 2),
                    isRequired: argError.error.isRequired,
                });
            }
        }
        const errorStr = `\n\n${chalk_1.default.red(`Invalid ${chalk_1.default.bold(`\`photon.${originalMethod || queryName}()\``)} invocation:`)}

${printJsonErrors_1.printJsonWithErrors(isTopLevelQuery ? { [topLevelQueryName]: select } : select, keyPaths, valuePaths, missingItems)}

${argErrors
            .filter(e => e.error.type !== 'missingArg' || e.error.isRequired)
            .map(this.printArgError)
            .join('\n')}
${fieldErrors.map(this.printFieldError).join('\n')}\n`;
        throw new InvalidClientInputError(errorStr);
    }
    /**
     * As we're allowing both single objects and array of objects for list inputs, we need to remove incorrect
     * zero indexes from the path
     * @param inputPath e.g. ['where', 'AND', 0, 'id']
     * @param select select object
     */
    normalizePath(inputPath, select) {
        const path = inputPath.slice();
        const newPath = [];
        let key;
        let pointer = select;
        // tslint:disable-next-line:no-conditional-assignment
        while ((key = path.shift()) !== undefined) {
            if (!Array.isArray(pointer) && key === 0) {
                continue;
            }
            pointer = pointer[key];
            newPath.push(key);
        }
        return newPath;
    }
}
exports.Document = Document;
class InvalidClientInputError extends Error {
}
class Field {
    constructor({ name, args, children, error }) {
        this.name = name;
        this.args = args;
        this.children = children;
        this.error = error;
        this.hasInvalidChild = children
            ? children.some(child => Boolean(child.error || child.hasInvalidArg || child.hasInvalidChild))
            : false;
        this.hasInvalidArg = args ? args.hasInvalidArg : false;
    }
    toString() {
        let str = this.name;
        // TODO: Decide if we should do this
        if (this.error) {
            return str + ' # INVALID_FIELD';
        }
        if (this.args && this.args.args && this.args.args.length > 0) {
            if (this.args.args.length === 1) {
                str += `(${this.args.toString()})`;
            }
            else {
                str += `(\n${indent_string_1.default(this.args.toString(), tab)}\n)`;
            }
        }
        if (this.children) {
            str += ` {
${indent_string_1.default(this.children.map(String).join('\n'), tab)}
}`;
        }
        return str;
    }
    collectErrors(prefix = 'select') {
        const fieldErrors = [];
        const argErrors = [];
        if (this.error) {
            fieldErrors.push({
                path: [this.name],
                error: this.error,
            });
        }
        // get all errors from fields
        if (this.children) {
            for (const child of this.children) {
                const errors = child.collectErrors();
                // Field -> Field always goes through a 'select'
                fieldErrors.push(...errors.fieldErrors.map(e => (Object.assign({}, e, { path: [this.name, 'select', ...e.path] }))));
                argErrors.push(...errors.argErrors.map(e => (Object.assign({}, e, { path: [this.name, 'select', ...e.path] }))));
            }
        }
        // get all errors from args
        if (this.args) {
            argErrors.push(...this.args.collectErrors().map(e => (Object.assign({}, e, { path: [this.name, ...e.path] }))));
        }
        return {
            fieldErrors,
            argErrors,
        };
    }
}
exports.Field = Field;
class Args {
    constructor(args = []) {
        this.args = args;
        this.hasInvalidArg = args ? args.some(arg => Boolean(arg.hasError)) : false;
    }
    toString() {
        if (this.args.length === 0) {
            return '';
        }
        return `${this.args.map(String).join('\n')}`;
    }
    collectErrors() {
        if (!this.hasInvalidArg) {
            return [];
        }
        return this.args.flatMap(arg => arg.collectErrors());
    }
}
exports.Args = Args;
/**
 * Custom stringify which turns undefined into null - needed by GraphQL
 * @param obj to stringify
 * @param _
 * @param tab
 */
function stringify(obj, _, tabbing, isEnum) {
    if (obj === undefined) {
        return null;
    }
    if (isEnum && typeof obj === 'string') {
        return obj;
    }
    if (isEnum && Array.isArray(obj)) {
        return `[${obj.join(', ')}]`;
    }
    return JSON.stringify(obj, _, tabbing);
}
class Arg {
    constructor({ key, value, argType, isEnum = false, error, schemaArg }) {
        this.key = key;
        this.value = value;
        this.argType = argType;
        this.isEnum = isEnum;
        this.error = error;
        this.schemaArg = schemaArg;
        this.hasError =
            Boolean(error) ||
                (value instanceof Args ? value.hasInvalidArg : false) ||
                (Array.isArray(value) && value.some(v => (v instanceof Args ? v.hasInvalidArg : false)));
    }
    _toString(value, key) {
        if (value instanceof Args) {
            return `${key}: {
${indent_string_1.default(value.toString(), 2)}
}`;
        }
        if (Array.isArray(value)) {
            const isScalar = !value.some(v => typeof v === 'object');
            return `${key}: [${isScalar ? '' : '\n'}${indent_string_1.default(value
                .map(nestedValue => {
                if (nestedValue instanceof Args) {
                    return `{\n${indent_string_1.default(nestedValue.toString(), tab)}\n}`;
                }
                return stringify(nestedValue, null, 2, this.isEnum);
            })
                .join(`,${isScalar ? ' ' : '\n'}`), isScalar ? 0 : tab)}${isScalar ? '' : '\n'}]`;
        }
        return `${key}: ${stringify(value, null, 2, this.isEnum)}`;
    }
    toString() {
        return this._toString(this.value, this.key);
    }
    collectErrors() {
        if (!this.hasError) {
            return [];
        }
        const errors = [];
        // add the own arg
        if (this.error) {
            errors.push({
                error: this.error,
                path: [this.key],
            });
        }
        if (Array.isArray(this.value)) {
            errors.push(...this.value.flatMap((val, index) => {
                if (!val.collectErrors) {
                    return [];
                }
                return val.collectErrors().map(e => {
                    return Object.assign({}, e, { path: [this.key, index, ...e.path] });
                });
            }));
        }
        // collect errors of children if there are any
        if (this.value instanceof Args) {
            errors.push(...this.value.collectErrors().map(e => (Object.assign({}, e, { path: [this.key, ...e.path] }))));
        }
        return errors;
    }
}
exports.Arg = Arg;
function makeDocument({ dmmf, rootTypeName, rootField, select }) {
    // console.log(stringifyInputType(input))
    const rootType = rootTypeName === 'query' ? dmmf.queryType : dmmf.mutationType;
    // Create a fake toplevel field for easier implementation
    const fakeRootField = {
        args: [],
        isList: false,
        isRequired: true,
        name: rootTypeName,
        type: rootType,
        kind: 'relation',
    };
    return new Document(rootTypeName, selectionToFields(dmmf, { [rootField]: select }, fakeRootField, [rootTypeName]));
}
exports.makeDocument = makeDocument;
function transformDocument(document) {
    function transformWhereArgs(args) {
        return new Args(args.args.flatMap(ar => {
            if (isArgsArray(ar.value)) {
                // long variable name to prevent shadowing
                const value = ar.value.map(argsInstance => {
                    return transformWhereArgs(argsInstance);
                });
                return new Arg(Object.assign({}, ar, { value }));
            }
            else if (ar.value instanceof Args) {
                if (ar.schemaArg && !ar.schemaArg.isRelationFilter) {
                    return ar.value.args.map(a => new Arg({
                        key: getFilterArgName(ar.key, a.key),
                        value: a.value,
                        /**
                         * This is an ugly hack. It assumes, that deeploy somewhere must be a valid inputType for
                         * this argument
                         */
                        argType: deep_set_1.deepGet(ar, ['value', 'args', '0', 'argType']),
                        schemaArg: ar.schemaArg,
                    }));
                }
            }
            return [ar];
        }));
    }
    function transformOrderArg(arg) {
        if (arg.value instanceof Args) {
            const orderArg = arg.value.args[0];
            return new Arg(Object.assign({}, arg, { isEnum: true, value: `${orderArg.key}_${orderArg.value.toString().toUpperCase()}` }));
        }
        return arg;
    }
    return visit_1.visit(document, {
        Arg: {
            enter(arg) {
                // if (arg.key === 'Albums') {
                //   console.log('Albums', arg)
                // }
                // if (arg.key === 'Tracks') {
                //   console.log('Tracks', arg)
                // }
                // if (arg.key === 'some') {
                //   console.log('some', arg)
                // }
                const { argType, schemaArg } = arg;
                if (!argType) {
                    return undefined;
                }
                if (isInputArgType(argType)) {
                    if (argType.isOrderType) {
                        return transformOrderArg(arg);
                    }
                    if (argType.isWhereType && schemaArg) {
                        let value;
                        if (isArgsArray(arg.value)) {
                            value = arg.value.map(val => transformWhereArgs(val));
                        }
                        else if (arg.value instanceof Args) {
                            value = transformWhereArgs(arg.value);
                        }
                        return new Arg(Object.assign({}, arg, { value }));
                    }
                }
                return undefined;
            },
        },
    });
}
exports.transformDocument = transformDocument;
function isArgsArray(input) {
    if (Array.isArray(input)) {
        return input.every(arg => arg instanceof Args);
    }
    return false;
}
function getFilterArgName(arg, filter) {
    if (filter === 'equals') {
        return arg;
    }
    return `${arg}_${filter}`;
}
function selectionToFields(dmmf, selection, schemaField, path) {
    const outputType = schemaField.type;
    return Object.entries(selection).reduce((acc, [name, value]) => {
        const field = outputType.fields.find(f => f.name === name);
        if (!field) {
            // if the field name is incorrect, we ignore the args and child fields altogether
            acc.push(new Field({
                name,
                children: [],
                // @ts-ignore
                error: {
                    type: 'invalidFieldName',
                    modelName: outputType.name,
                    providedName: name,
                    didYouMean: common_1.getSuggestion(name, outputType.fields.map(f => f.name)),
                },
            }));
            return acc;
        }
        if (typeof value !== 'boolean' && field.kind === 'scalar') {
            acc.push(new Field({
                name,
                children: [],
                error: {
                    type: 'invalidFieldType',
                    modelName: outputType.name,
                    fieldName: name,
                    providedValue: value,
                },
            }));
            return acc;
        }
        if (value === false) {
            return acc;
        }
        const argsWithoutSelect = typeof value === 'object' ? omit_1.omit(value, 'select') : undefined;
        const args = argsWithoutSelect
            ? objectToArgs(argsWithoutSelect, field, [], typeof field === 'string' ? undefined : field.type)
            : undefined;
        const isRelation = field.kind === 'relation';
        const defaultSelection = isRelation ? getDefaultSelection(field.type) : null;
        const select = deep_extend_1.deepExtend(defaultSelection, value ? value.select : {});
        const children = select !== false && isRelation ? selectionToFields(dmmf, select, field, [...path, name]) : undefined;
        acc.push(new Field({ name, args, children }));
        return acc;
    }, []);
}
exports.selectionToFields = selectionToFields;
function getDefaultSelection(outputType) {
    return outputType.fields.reduce((acc, f) => {
        if (f.kind === 'scalar') {
            acc[f.name] = true;
        }
        else {
            // otherwise field is a relation. Only continue if it's an embedded type
            // as normal types don't end up in the default selection
            if (f.type.isEmbedded) {
                acc[f.name] = { select: getDefaultSelection(f.type) };
            }
        }
        return acc;
    }, {});
}
function getInvalidTypeArg(key, value, arg, bestFittingType) {
    return new Arg({
        key,
        value,
        isEnum: arg.isEnum,
        argType: bestFittingType,
        error: {
            type: 'invalidType',
            providedValue: value,
            argName: key,
            requiredType: {
                isList: arg.isList,
                isEnum: arg.isEnum,
                isRequired: arg.isRequired,
                isScalar: arg.isScalar,
                types: arg.type,
                bestFittingType,
            },
        },
    });
}
function hasCorrectScalarType(value, arg, type) {
    const expectedType = common_1.wrapWithList(common_1.stringifyGraphQLType(type), arg.isList);
    const graphQLType = common_1.getGraphQLType(value, type);
    // DateTime is a subset of string
    if (graphQLType === 'DateTime' && expectedType === 'String') {
        return true;
    }
    if (graphQLType === 'String' && expectedType === 'ID') {
        return true;
    }
    // Int is a subset of Float
    if (graphQLType === 'Int' && expectedType === 'Float') {
        return true;
    }
    // Int is a subset of Long
    if (graphQLType === 'Int' && expectedType === 'Long') {
        return true;
    }
    if (graphQLType === expectedType) {
        return true;
    }
    return false;
}
const cleanObject = obj => filterObject_1.filterObject(obj, (k, v) => v !== undefined);
function valueToArg(key, value, arg) {
    if (typeof value === 'undefined') {
        // the arg is undefined and not required - we're fine
        if (!arg.isRequired) {
            return null;
        }
        // the provided value is 'undefined' but shouldn't be
        return new Arg({
            key,
            value,
            isEnum: arg.isEnum,
            error: {
                type: 'missingArg',
                missingName: key,
                isScalar: arg.isScalar,
                isEnum: arg.isEnum,
                isList: arg.isList,
                missingType: arg.type,
                isRequired: true,
                atLeastOne: false,
                atMostOne: false,
            },
        });
    }
    if (!arg.isList) {
        const args = arg.type.map(t => {
            if (isInputArgType(t)) {
                if (typeof value !== 'object') {
                    return getInvalidTypeArg(key, value, arg, t);
                }
                else {
                    const val = cleanObject(value);
                    let error;
                    const keys = Object.keys(val || {});
                    const numKeys = keys.length;
                    if (numKeys === 0 && t.atLeastOne) {
                        error = {
                            type: 'atLeastOne',
                            key,
                            inputType: t,
                        };
                    }
                    if (numKeys > 1 && t.atMostOne) {
                        error = {
                            type: 'atMostOne',
                            key,
                            inputType: t,
                            providedKeys: keys,
                        };
                    }
                    return new Arg({
                        key,
                        value: objectToArgs(val, t, arg.type),
                        isEnum: arg.isEnum,
                        error,
                        argType: t,
                        schemaArg: arg,
                    });
                }
            }
            else {
                return scalarToArg(key, value, arg, t);
            }
        });
        // is it just one possible type? Then no matter what just return that one
        if (args.length === 1) {
            return args[0];
        }
        // do we have more then one, but does it fit one of the args? Then let's just take that one arg
        const argWithoutError = args.find(a => !a.hasError);
        if (argWithoutError) {
            return argWithoutError;
        }
        const hasSameKind = (argType, val) => {
            if (val === null && (argType === 'null' || !isInputArgType(argType))) {
                return true;
            }
            return isInputArgType(argType) ? typeof val === 'object' : typeof val !== 'object';
        };
        /**
         * If there are more than 1 args, do the following:
         * First check if there are any possible arg types which at least have the
         * correct base type (scalar, null or object)
         * Take either these, or if they don't exist just again the normal args and
         * take the arg with the minimum amount of errors
         */
        if (args.length > 1) {
            const argsWithSameKind = args.filter(a => hasSameKind(a.argType, value));
            const argsToFilter = argsWithSameKind.length > 0 ? argsWithSameKind : args;
            const argWithMinimumErrors = argsToFilter.reduce((acc, curr) => {
                const numErrors = curr.collectErrors().length;
                if (numErrors < acc.numErrors) {
                    return {
                        arg: curr,
                        numErrors,
                    };
                }
                return acc;
            }, {
                arg: null,
                numErrors: Infinity,
            });
            return argWithMinimumErrors.arg;
        }
    }
    if (arg.type.length > 1) {
        throw new Error(`List types with union input types are not supported`);
    }
    // the provided arg should be a list, but isn't
    // that's fine for us as we can just turn this into a list with a single item
    // and GraphQL even allows this. We're going the conservative route though
    // and actually generate the [] around the value
    if (!Array.isArray(value)) {
        value = [value];
    }
    if (arg.isScalar) {
        // if no value is incorrect
        return scalarToArg(key, value, arg, arg.type[0]);
    }
    if (!arg.isScalar) {
        const inputType = arg.type[0];
        const hasAtLeastOneError = inputType.atLeastOne ? value.some(v => Object.keys(cleanObject(v)).length === 0) : false;
        const error = hasAtLeastOneError
            ? {
                inputType,
                key,
                type: 'atLeastOne',
            }
            : undefined;
        return new Arg({
            key,
            value: value.map(v => {
                if (typeof v !== 'object' || !value) {
                    return getInvalidTypeArg(key, v, arg, arg.type[0]);
                }
                return objectToArgs(v, arg.type[0]);
            }),
            isEnum: arg.isEnum,
            argType: arg.type[0],
            schemaArg: arg,
            error,
        });
    }
    // TODO: Decide for better default case
    throw new Error('Oops. This must not happen');
}
function isInputArgType(argType) {
    if (typeof argType === 'string') {
        return false;
    }
    if (argType.hasOwnProperty('values')) {
        return false;
    }
    return true;
}
exports.isInputArgType = isInputArgType;
function scalarToArg(key, value, arg, type) {
    if (hasCorrectScalarType(value, arg, type)) {
        return new Arg({ key, value, isEnum: arg.isEnum, argType: type, schemaArg: arg });
    }
    return getInvalidTypeArg(key, value, arg, type);
}
function objectToArgs(initialObj, inputType, possibilities, outputType) {
    // filter out undefined values and treat them if they weren't provided
    // TODO: think about using JSON.parse(JSON.stringify()) upfront instead to simplify things
    const obj = cleanObject(initialObj);
    const { args } = inputType;
    const requiredArgs = args.filter(arg => arg.isRequired).map(arg => [arg.name, undefined]);
    const entries = common_1.unionBy(Object.entries(obj || {}), requiredArgs, a => a[0]);
    const argsList = entries.reduce((acc, [argName, value]) => {
        const schemaArg = args.find(a => a.name === argName);
        if (!schemaArg) {
            const didYouMeanField = typeof value === 'boolean' && outputType && outputType.fields.some(f => f.name === argName) ? argName : null;
            acc.push(new Arg({
                key: argName,
                value,
                error: {
                    type: 'invalidName',
                    providedName: argName,
                    providedValue: value,
                    didYouMeanField,
                    didYouMeanArg: (!didYouMeanField && common_1.getSuggestion(argName, [...args.map(a => a.name), 'select'])) || undefined,
                    originalType: inputType,
                    possibilities,
                    outputType,
                },
            }));
            return acc;
        }
        const arg = valueToArg(argName, value, schemaArg);
        if (arg) {
            acc.push(arg);
        }
        return acc;
    }, []);
    // Also show optional neighbour args, if there is any arg missing
    if ((entries.length === 0 && inputType.atLeastOne) ||
        argsList.find(arg => arg.error && arg.error.type === 'missingArg')) {
        const optionalMissingArgs = inputType.args.filter(arg => !entries.some(([entry]) => entry === arg.name));
        argsList.push(...optionalMissingArgs.map(arg => new Arg({
            key: arg.name,
            value: undefined,
            isEnum: arg.isEnum,
            error: {
                type: 'missingArg',
                isEnum: arg.isEnum,
                isList: arg.isList,
                isScalar: arg.isScalar,
                missingName: arg.name,
                missingType: arg.type,
                isRequired: false,
                atLeastOne: inputType.atLeastOne || false,
                atMostOne: inputType.atMostOne || false,
            },
        })));
    }
    return new Args(argsList);
}


/***/ }),

/***/ 484:
/***/ (function(module) {

"use strict";


module.exports = (string, count = 1, options) => {
	options = {
		indent: ' ',
		includeEmptyLines: false,
		...options
	};

	if (typeof string !== 'string') {
		throw new TypeError(
			`Expected \`input\` to be a \`string\`, got \`${typeof string}\``
		);
	}

	if (typeof count !== 'number') {
		throw new TypeError(
			`Expected \`count\` to be a \`number\`, got \`${typeof count}\``
		);
	}

	if (typeof options.indent !== 'string') {
		throw new TypeError(
			`Expected \`options.indent\` to be a \`string\`, got \`${typeof options.indent}\``
		);
	}

	if (count === 0) {
		return string;
	}

	const regex = options.includeEmptyLines ? /^/gm : /^(?!\s*$)/gm;

	return string.replace(regex, options.indent.repeat(count));
};


/***/ }),

/***/ 494:
/***/ (function(__unusedmodule, exports) {

"use strict";

// Taken from https://github.com/unclechu/node-deep-extend/blob/master/lib/deep-extend.js
// es2017-ified, now it's about 2.5 times faster
/*!
 * @description Recursive object extending
 * @author Viacheslav Lotsmanov <lotsmanov89@gmail.com>
 * @license MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2018 Viacheslav Lotsmanov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
Object.defineProperty(exports, "__esModule", { value: true });
/* tslint:disable */
function isSpecificValue(val) {
    return val instanceof Buffer || val instanceof Date || val instanceof RegExp ? true : false;
}
function cloneSpecificValue(val) {
    if (val instanceof Buffer) {
        const x = Buffer.alloc ? Buffer.alloc(val.length) : new Buffer(val.length);
        val.copy(x);
        return x;
    }
    else if (val instanceof Date) {
        return new Date(val.getTime());
    }
    else if (val instanceof RegExp) {
        return new RegExp(val);
    }
    else {
        throw new Error('Unexpected situation');
    }
}
/**
 * Recursive cloning array.
 */
function deepCloneArray(arr) {
    const clone = [];
    arr.forEach(function (item, index) {
        if (typeof item === 'object' && item !== null) {
            if (Array.isArray(item)) {
                clone[index] = deepCloneArray(item);
            }
            else if (isSpecificValue(item)) {
                clone[index] = cloneSpecificValue(item);
            }
            else {
                clone[index] = exports.deepExtend({}, item);
            }
        }
        else {
            clone[index] = item;
        }
    });
    return clone;
}
function safeGetProperty(object, property) {
    return property === '__proto__' ? undefined : object[property];
}
/**
 * Extening object that entered in first argument.
 *
 * Returns extended object or false if have no target object or incorrect type.
 *
 * If you wish to clone source object (without modify it), just use empty new
 * object as first argument, like this:
 *   deepExtend({}, yourObj_1, [yourObj_N]);
 */
exports.deepExtend = function (target, ...args) {
    if (!target || typeof target !== 'object') {
        return false;
    }
    if (args.length === 0) {
        return target;
    }
    let val, src;
    for (const obj of args) {
        // skip argument if isn't an object, is null, or is an array
        if (typeof obj !== 'object' || obj === null || Array.isArray(obj)) {
            continue;
        }
        for (const key of Object.keys(obj)) {
            src = safeGetProperty(target, key); // source value
            val = safeGetProperty(obj, key); // new value
            // recursion prevention
            if (val === target) {
                continue;
                /**
                 * if new value isn't object then just overwrite by new value
                 * instead of extending.
                 */
            }
            else if (typeof val !== 'object' || val === null) {
                target[key] = val;
                continue;
                // just clone arrays (and recursive clone objects inside)
            }
            else if (Array.isArray(val)) {
                target[key] = deepCloneArray(val);
                continue;
                // custom cloning and overwrite for specific objects
            }
            else if (isSpecificValue(val)) {
                target[key] = cloneSpecificValue(val);
                continue;
                // overwrite by new value if source isn't object or array
            }
            else if (typeof src !== 'object' || src === null || Array.isArray(src)) {
                target[key] = exports.deepExtend({}, val);
                continue;
                // source value and new value is objects both, extending...
            }
            else {
                target[key] = exports.deepExtend(src, val);
                continue;
            }
        }
    }
    return target;
};
// @ts-ignore-end


/***/ }),

/***/ 500:
/***/ (function(module) {

"use strict";

module.exports = (function()
{
  function _min(d0, d1, d2, bx, ay)
  {
    return d0 < d1 || d2 < d1
        ? d0 > d2
            ? d2 + 1
            : d0 + 1
        : bx === ay
            ? d1
            : d1 + 1;
  }

  return function(a, b)
  {
    if (a === b) {
      return 0;
    }

    if (a.length > b.length) {
      var tmp = a;
      a = b;
      b = tmp;
    }

    var la = a.length;
    var lb = b.length;

    while (la > 0 && (a.charCodeAt(la - 1) === b.charCodeAt(lb - 1))) {
      la--;
      lb--;
    }

    var offset = 0;

    while (offset < la && (a.charCodeAt(offset) === b.charCodeAt(offset))) {
      offset++;
    }

    la -= offset;
    lb -= offset;

    if (la === 0 || lb < 3) {
      return lb;
    }

    var x = 0;
    var y;
    var d0;
    var d1;
    var d2;
    var d3;
    var dd;
    var dy;
    var ay;
    var bx0;
    var bx1;
    var bx2;
    var bx3;

    var vector = [];

    for (y = 0; y < la; y++) {
      vector.push(y + 1);
      vector.push(a.charCodeAt(offset + y));
    }

    var len = vector.length - 1;

    for (; x < lb - 3;) {
      bx0 = b.charCodeAt(offset + (d0 = x));
      bx1 = b.charCodeAt(offset + (d1 = x + 1));
      bx2 = b.charCodeAt(offset + (d2 = x + 2));
      bx3 = b.charCodeAt(offset + (d3 = x + 3));
      dd = (x += 4);
      for (y = 0; y < len; y += 2) {
        dy = vector[y];
        ay = vector[y + 1];
        d0 = _min(dy, d0, d1, bx0, ay);
        d1 = _min(d0, d1, d2, bx1, ay);
        d2 = _min(d1, d2, d3, bx2, ay);
        dd = _min(d2, d3, dd, bx3, ay);
        vector[y] = dd;
        d3 = d2;
        d2 = d1;
        d1 = d0;
        d0 = dy;
      }
    }

    for (; x < lb;) {
      bx0 = b.charCodeAt(offset + (d0 = x));
      dd = ++x;
      for (y = 0; y < len; y += 2) {
        dy = vector[y];
        vector[y] = dd = _min(dy, d0, dd, bx0, vector[y + 1]);
        d0 = dy;
      }
    }

    return dd;
  };
})();



/***/ }),

/***/ 510:
/***/ (function(module) {

"use strict";


var matchOperatorsRe = /[|\\{}()[\]^$+*?.]/g;

module.exports = function (str) {
	if (typeof str !== 'string') {
		throw new TypeError('Expected a string');
	}

	return str.replace(matchOperatorsRe, '\\$&');
};


/***/ }),

/***/ 583:
/***/ (function(module, exports, __webpack_require__) {

/* eslint-env browser */

/**
 * This is the web browser implementation of `debug()`.
 */

exports.log = log;
exports.formatArgs = formatArgs;
exports.save = save;
exports.load = load;
exports.useColors = useColors;
exports.storage = localstorage();

/**
 * Colors.
 */

exports.colors = [
	'#0000CC',
	'#0000FF',
	'#0033CC',
	'#0033FF',
	'#0066CC',
	'#0066FF',
	'#0099CC',
	'#0099FF',
	'#00CC00',
	'#00CC33',
	'#00CC66',
	'#00CC99',
	'#00CCCC',
	'#00CCFF',
	'#3300CC',
	'#3300FF',
	'#3333CC',
	'#3333FF',
	'#3366CC',
	'#3366FF',
	'#3399CC',
	'#3399FF',
	'#33CC00',
	'#33CC33',
	'#33CC66',
	'#33CC99',
	'#33CCCC',
	'#33CCFF',
	'#6600CC',
	'#6600FF',
	'#6633CC',
	'#6633FF',
	'#66CC00',
	'#66CC33',
	'#9900CC',
	'#9900FF',
	'#9933CC',
	'#9933FF',
	'#99CC00',
	'#99CC33',
	'#CC0000',
	'#CC0033',
	'#CC0066',
	'#CC0099',
	'#CC00CC',
	'#CC00FF',
	'#CC3300',
	'#CC3333',
	'#CC3366',
	'#CC3399',
	'#CC33CC',
	'#CC33FF',
	'#CC6600',
	'#CC6633',
	'#CC9900',
	'#CC9933',
	'#CCCC00',
	'#CCCC33',
	'#FF0000',
	'#FF0033',
	'#FF0066',
	'#FF0099',
	'#FF00CC',
	'#FF00FF',
	'#FF3300',
	'#FF3333',
	'#FF3366',
	'#FF3399',
	'#FF33CC',
	'#FF33FF',
	'#FF6600',
	'#FF6633',
	'#FF9900',
	'#FF9933',
	'#FFCC00',
	'#FFCC33'
];

/**
 * Currently only WebKit-based Web Inspectors, Firefox >= v31,
 * and the Firebug extension (any Firefox version) are known
 * to support "%c" CSS customizations.
 *
 * TODO: add a `localStorage` variable to explicitly enable/disable colors
 */

// eslint-disable-next-line complexity
function useColors() {
	// NB: In an Electron preload script, document will be defined but not fully
	// initialized. Since we know we're in Chrome, we'll just detect this case
	// explicitly
	if (typeof window !== 'undefined' && window.process && (window.process.type === 'renderer' || window.process.__nwjs)) {
		return true;
	}

	// Internet Explorer and Edge do not support colors.
	if (typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/(edge|trident)\/(\d+)/)) {
		return false;
	}

	// Is webkit? http://stackoverflow.com/a/16459606/376773
	// document is undefined in react-native: https://github.com/facebook/react-native/pull/1632
	return (typeof document !== 'undefined' && document.documentElement && document.documentElement.style && document.documentElement.style.WebkitAppearance) ||
		// Is firebug? http://stackoverflow.com/a/398120/376773
		(typeof window !== 'undefined' && window.console && (window.console.firebug || (window.console.exception && window.console.table))) ||
		// Is firefox >= v31?
		// https://developer.mozilla.org/en-US/docs/Tools/Web_Console#Styling_messages
		(typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/firefox\/(\d+)/) && parseInt(RegExp.$1, 10) >= 31) ||
		// Double check webkit in userAgent just in case we are in a worker
		(typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/applewebkit\/(\d+)/));
}

/**
 * Colorize log arguments if enabled.
 *
 * @api public
 */

function formatArgs(args) {
	args[0] = (this.useColors ? '%c' : '') +
		this.namespace +
		(this.useColors ? ' %c' : ' ') +
		args[0] +
		(this.useColors ? '%c ' : ' ') +
		'+' + module.exports.humanize(this.diff);

	if (!this.useColors) {
		return;
	}

	const c = 'color: ' + this.color;
	args.splice(1, 0, c, 'color: inherit');

	// The final "%c" is somewhat tricky, because there could be other
	// arguments passed either before or after the %c, so we need to
	// figure out the correct index to insert the CSS into
	let index = 0;
	let lastC = 0;
	args[0].replace(/%[a-zA-Z%]/g, match => {
		if (match === '%%') {
			return;
		}
		index++;
		if (match === '%c') {
			// We only are interested in the *last* %c
			// (the user may have provided their own)
			lastC = index;
		}
	});

	args.splice(lastC, 0, c);
}

/**
 * Invokes `console.log()` when available.
 * No-op when `console.log` is not a "function".
 *
 * @api public
 */
function log(...args) {
	// This hackery is required for IE8/9, where
	// the `console.log` function doesn't have 'apply'
	return typeof console === 'object' &&
		console.log &&
		console.log(...args);
}

/**
 * Save `namespaces`.
 *
 * @param {String} namespaces
 * @api private
 */
function save(namespaces) {
	try {
		if (namespaces) {
			exports.storage.setItem('debug', namespaces);
		} else {
			exports.storage.removeItem('debug');
		}
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}
}

/**
 * Load `namespaces`.
 *
 * @return {String} returns the previously persisted debug modes
 * @api private
 */
function load() {
	let r;
	try {
		r = exports.storage.getItem('debug');
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}

	// If debug isn't set in LS, and we're in Electron, try to load $DEBUG
	if (!r && typeof process !== 'undefined' && 'env' in process) {
		r = process.env.DEBUG;
	}

	return r;
}

/**
 * Localstorage attempts to return the localstorage.
 *
 * This is necessary because safari throws
 * when a user disables cookies/localstorage
 * and you attempt to access it.
 *
 * @return {LocalStorage}
 * @api private
 */

function localstorage() {
	try {
		// TVMLKit (Apple TV JS Runtime) does not have a window object, just localStorage in the global context
		// The Browser also has localStorage in the global context.
		return localStorage;
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}
}

module.exports = __webpack_require__(171)(exports);

const {formatters} = module.exports;

/**
 * Map %j to `JSON.stringify()`, since no Web Inspectors do that by default.
 */

formatters.j = function (v) {
	try {
		return JSON.stringify(v);
	} catch (error) {
		return '[UnexpectedJSONParseError]: ' + error.message;
	}
};


/***/ }),

/***/ 587:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
const common_1 = __webpack_require__(957);
class DMMFClass {
    constructor({ datamodel, schema, mappings }) {
        this.outputTypeMap = {};
        this.outputTypeToMergedOutputType = (outputType) => {
            const model = this.modelMap[outputType.name];
            return Object.assign({}, outputType, { isEmbedded: model ? model.isEmbedded : false, fields: outputType.fields });
        };
        this.datamodel = datamodel;
        this.schema = schema;
        this.mappings = mappings;
        this.enumMap = this.getEnumMap();
        this.queryType = this.getQueryType();
        this.mutationType = this.getMutationType();
        this.schema.outputTypes.push(this.queryType); // create "virtual" query type
        this.schema.outputTypes.push(this.mutationType); // create "virtual" mutation type
        this.modelMap = this.getModelMap();
        this.outputTypes = this.getOutputTypes();
        this.resolveOutputTypes(this.outputTypes);
        this.inputTypes = this.schema.inputTypes;
        this.resolveInputTypes(this.inputTypes);
        this.inputTypeMap = this.getInputTypeMap();
        this.resolveFieldArgumentTypes(this.outputTypes, this.inputTypeMap);
        this.outputTypeMap = this.getMergedOutputTypeMap();
        // needed as references are not kept
        this.queryType = this.outputTypeMap.Query;
        this.mutationType = this.outputTypeMap.Mutation;
        this.outputTypes = this.outputTypes;
    }
    getField(fieldName) {
        return (
        // TODO: create lookup table for Query and Mutation
        this.queryType.fields.find(f => f.name === fieldName) || this.mutationType.fields.find(f => f.name === fieldName));
    }
    resolveOutputTypes(types) {
        for (const typeA of types) {
            for (const fieldA of typeA.fields) {
                for (const typeB of types) {
                    if (typeof fieldA.type === 'string') {
                        if (fieldA.type === typeB.name) {
                            fieldA.type = typeB;
                        }
                        else if (this.enumMap[fieldA.type]) {
                            fieldA.type = this.enumMap[fieldA.type];
                        }
                    }
                }
            }
        }
    }
    resolveInputTypes(types) {
        for (const typeA of types) {
            for (const fieldA of typeA.args) {
                for (const typeB of types) {
                    fieldA.type.forEach((type, index) => {
                        if (typeof type === 'string') {
                            if (type === typeB.name) {
                                fieldA.type[index] = typeB;
                            }
                            else if (this.enumMap[type]) {
                                fieldA.type[index] = this.enumMap[type];
                            }
                        }
                    });
                }
            }
        }
    }
    resolveFieldArgumentTypes(types, inputTypeMap) {
        for (const type of types) {
            for (const field of type.fields) {
                for (const arg of field.args) {
                    arg.type.forEach((t, index) => {
                        if (typeof t === 'string') {
                            if (inputTypeMap[t]) {
                                arg.type[index] = inputTypeMap[t];
                            }
                            else if (this.enumMap[t]) {
                                arg.type[index] = this.enumMap[t];
                            }
                        }
                    });
                }
            }
        }
    }
    getQueryType() {
        return {
            name: 'Query',
            fields: this.schema.queries
                .map(queryToSchemaField)
                .filter(f => !f.name.endsWith('Connection') && f.name !== 'Node'),
            isEmbedded: false,
        };
    }
    getMutationType() {
        return {
            name: 'Mutation',
            fields: this.schema.mutations.map(queryToSchemaField),
            isEmbedded: false,
        };
    }
    getOutputTypes() {
        return this.schema.outputTypes.map(this.outputTypeToMergedOutputType);
    }
    getEnumMap() {
        return common_1.keyBy(this.schema.enums, e => e.name);
    }
    getModelMap() {
        return common_1.keyBy(this.datamodel.models, m => m.name);
    }
    getMergedOutputTypeMap() {
        return common_1.keyBy(this.outputTypes, t => t.name);
    }
    getInputTypeMap() {
        return common_1.keyBy(this.schema.inputTypes, t => t.name);
    }
}
exports.DMMFClass = DMMFClass;
const queryToSchemaField = (q) => ({
    name: q.name,
    args: q.args,
    isList: q.output.isList,
    isRequired: q.output.isRequired,
    type: q.output.name,
    kind: 'relation',
});


/***/ }),

/***/ 589:
/***/ (function(module) {

module.exports = require("path");

/***/ }),

/***/ 620:
/***/ (function(module, exports, __webpack_require__) {

/* eslint-env browser */

/**
 * This is the web browser implementation of `debug()`.
 */

exports.log = log;
exports.formatArgs = formatArgs;
exports.save = save;
exports.load = load;
exports.useColors = useColors;
exports.storage = localstorage();

/**
 * Colors.
 */

exports.colors = [
	'#0000CC',
	'#0000FF',
	'#0033CC',
	'#0033FF',
	'#0066CC',
	'#0066FF',
	'#0099CC',
	'#0099FF',
	'#00CC00',
	'#00CC33',
	'#00CC66',
	'#00CC99',
	'#00CCCC',
	'#00CCFF',
	'#3300CC',
	'#3300FF',
	'#3333CC',
	'#3333FF',
	'#3366CC',
	'#3366FF',
	'#3399CC',
	'#3399FF',
	'#33CC00',
	'#33CC33',
	'#33CC66',
	'#33CC99',
	'#33CCCC',
	'#33CCFF',
	'#6600CC',
	'#6600FF',
	'#6633CC',
	'#6633FF',
	'#66CC00',
	'#66CC33',
	'#9900CC',
	'#9900FF',
	'#9933CC',
	'#9933FF',
	'#99CC00',
	'#99CC33',
	'#CC0000',
	'#CC0033',
	'#CC0066',
	'#CC0099',
	'#CC00CC',
	'#CC00FF',
	'#CC3300',
	'#CC3333',
	'#CC3366',
	'#CC3399',
	'#CC33CC',
	'#CC33FF',
	'#CC6600',
	'#CC6633',
	'#CC9900',
	'#CC9933',
	'#CCCC00',
	'#CCCC33',
	'#FF0000',
	'#FF0033',
	'#FF0066',
	'#FF0099',
	'#FF00CC',
	'#FF00FF',
	'#FF3300',
	'#FF3333',
	'#FF3366',
	'#FF3399',
	'#FF33CC',
	'#FF33FF',
	'#FF6600',
	'#FF6633',
	'#FF9900',
	'#FF9933',
	'#FFCC00',
	'#FFCC33'
];

/**
 * Currently only WebKit-based Web Inspectors, Firefox >= v31,
 * and the Firebug extension (any Firefox version) are known
 * to support "%c" CSS customizations.
 *
 * TODO: add a `localStorage` variable to explicitly enable/disable colors
 */

// eslint-disable-next-line complexity
function useColors() {
	// NB: In an Electron preload script, document will be defined but not fully
	// initialized. Since we know we're in Chrome, we'll just detect this case
	// explicitly
	if (typeof window !== 'undefined' && window.process && (window.process.type === 'renderer' || window.process.__nwjs)) {
		return true;
	}

	// Internet Explorer and Edge do not support colors.
	if (typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/(edge|trident)\/(\d+)/)) {
		return false;
	}

	// Is webkit? http://stackoverflow.com/a/16459606/376773
	// document is undefined in react-native: https://github.com/facebook/react-native/pull/1632
	return (typeof document !== 'undefined' && document.documentElement && document.documentElement.style && document.documentElement.style.WebkitAppearance) ||
		// Is firebug? http://stackoverflow.com/a/398120/376773
		(typeof window !== 'undefined' && window.console && (window.console.firebug || (window.console.exception && window.console.table))) ||
		// Is firefox >= v31?
		// https://developer.mozilla.org/en-US/docs/Tools/Web_Console#Styling_messages
		(typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/firefox\/(\d+)/) && parseInt(RegExp.$1, 10) >= 31) ||
		// Double check webkit in userAgent just in case we are in a worker
		(typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/applewebkit\/(\d+)/));
}

/**
 * Colorize log arguments if enabled.
 *
 * @api public
 */

function formatArgs(args) {
	args[0] = (this.useColors ? '%c' : '') +
		this.namespace +
		(this.useColors ? ' %c' : ' ') +
		args[0] +
		(this.useColors ? '%c ' : ' ') +
		'+' + module.exports.humanize(this.diff);

	if (!this.useColors) {
		return;
	}

	const c = 'color: ' + this.color;
	args.splice(1, 0, c, 'color: inherit');

	// The final "%c" is somewhat tricky, because there could be other
	// arguments passed either before or after the %c, so we need to
	// figure out the correct index to insert the CSS into
	let index = 0;
	let lastC = 0;
	args[0].replace(/%[a-zA-Z%]/g, match => {
		if (match === '%%') {
			return;
		}
		index++;
		if (match === '%c') {
			// We only are interested in the *last* %c
			// (the user may have provided their own)
			lastC = index;
		}
	});

	args.splice(lastC, 0, c);
}

/**
 * Invokes `console.log()` when available.
 * No-op when `console.log` is not a "function".
 *
 * @api public
 */
function log(...args) {
	// This hackery is required for IE8/9, where
	// the `console.log` function doesn't have 'apply'
	return typeof console === 'object' &&
		console.log &&
		console.log(...args);
}

/**
 * Save `namespaces`.
 *
 * @param {String} namespaces
 * @api private
 */
function save(namespaces) {
	try {
		if (namespaces) {
			exports.storage.setItem('debug', namespaces);
		} else {
			exports.storage.removeItem('debug');
		}
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}
}

/**
 * Load `namespaces`.
 *
 * @return {String} returns the previously persisted debug modes
 * @api private
 */
function load() {
	let r;
	try {
		r = exports.storage.getItem('debug');
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}

	// If debug isn't set in LS, and we're in Electron, try to load $DEBUG
	if (!r && typeof process !== 'undefined' && 'env' in process) {
		r = process.env.DEBUG;
	}

	return r;
}

/**
 * Localstorage attempts to return the localstorage.
 *
 * This is necessary because safari throws
 * when a user disables cookies/localstorage
 * and you attempt to access it.
 *
 * @return {LocalStorage}
 * @api private
 */

function localstorage() {
	try {
		// TVMLKit (Apple TV JS Runtime) does not have a window object, just localStorage in the global context
		// The Browser also has localStorage in the global context.
		return localStorage;
	} catch (error) {
		// Swallow
		// XXX (@Qix-) should we be logging these?
	}
}

module.exports = __webpack_require__(247)(exports);

const {formatters} = module.exports;

/**
 * Map %j to `JSON.stringify()`, since no Web Inspectors do that by default.
 */

formatters.j = function (v) {
	try {
		return JSON.stringify(v);
	} catch (error) {
		return '[UnexpectedJSONParseError]: ' + error.message;
	}
};


/***/ }),

/***/ 622:
/***/ (function(module, __unusedexports, __webpack_require__) {

var conversions = __webpack_require__(752);

/*
	this function routes a model to all other models.

	all functions that are routed have a property `.conversion` attached
	to the returned synthetic function. This property is an array
	of strings, each with the steps in between the 'from' and 'to'
	color models (inclusive).

	conversions that are not possible simply are not included.
*/

function buildGraph() {
	var graph = {};
	// https://jsperf.com/object-keys-vs-for-in-with-closure/3
	var models = Object.keys(conversions);

	for (var len = models.length, i = 0; i < len; i++) {
		graph[models[i]] = {
			// http://jsperf.com/1-vs-infinity
			// micro-opt, but this is simple.
			distance: -1,
			parent: null
		};
	}

	return graph;
}

// https://en.wikipedia.org/wiki/Breadth-first_search
function deriveBFS(fromModel) {
	var graph = buildGraph();
	var queue = [fromModel]; // unshift -> queue -> pop

	graph[fromModel].distance = 0;

	while (queue.length) {
		var current = queue.pop();
		var adjacents = Object.keys(conversions[current]);

		for (var len = adjacents.length, i = 0; i < len; i++) {
			var adjacent = adjacents[i];
			var node = graph[adjacent];

			if (node.distance === -1) {
				node.distance = graph[current].distance + 1;
				node.parent = current;
				queue.unshift(adjacent);
			}
		}
	}

	return graph;
}

function link(from, to) {
	return function (args) {
		return to(from(args));
	};
}

function wrapConversion(toModel, graph) {
	var path = [graph[toModel].parent, toModel];
	var fn = conversions[graph[toModel].parent][toModel];

	var cur = graph[toModel].parent;
	while (graph[cur].parent) {
		path.unshift(graph[cur].parent);
		fn = link(conversions[graph[cur].parent][cur], fn);
		cur = graph[cur].parent;
	}

	fn.conversion = path;
	return fn;
}

module.exports = function (fromModel) {
	var graph = deriveBFS(fromModel);
	var conversion = {};

	var models = Object.keys(graph);
	for (var len = models.length, i = 0; i < len; i++) {
		var toModel = models[i];
		var node = graph[toModel];

		if (node.parent === null) {
			// no possible conversion, or this node is the source model.
			continue;
		}

		conversion[toModel] = wrapConversion(toModel, graph);
	}

	return conversion;
};



/***/ }),

/***/ 656:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";

const ansiRegex = __webpack_require__(446);

const stripAnsi = string => typeof string === 'string' ? string.replace(ansiRegex(), '') : string;

module.exports = stripAnsi;
module.exports.default = stripAnsi;


/***/ }),

/***/ 659:
/***/ (function(__unusedmodule, exports) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
function filterObject(obj, cb) {
    const newObj = {};
    for (const key in obj) {
        const value = obj[key];
        if (obj.hasOwnProperty(key) && cb(key, value)) {
            newObj[key] = value;
        }
    }
    return newObj;
}
exports.filterObject = filterObject;


/***/ }),

/***/ 688:
/***/ (function(module) {

module.exports = require("stream");

/***/ }),

/***/ 717:
/***/ (function(__unusedmodule, exports) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (object) => Object
    .getOwnPropertySymbols(object)
    .filter((keySymbol) => object.propertyIsEnumerable(keySymbol));
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZXguanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyIuLi9zcmMvaW5kZXgudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQSxrQkFBZSxDQUFDLE1BQWMsRUFBWSxFQUFFLENBQUMsTUFBTTtLQUNoRCxxQkFBcUIsQ0FBQyxNQUFNLENBQUM7S0FDN0IsTUFBTSxDQUFDLENBQUMsU0FBUyxFQUFXLEVBQUUsQ0FBQyxNQUFNLENBQUMsb0JBQW9CLENBQUMsU0FBUyxDQUFDLENBQUMsQ0FBQSIsInNvdXJjZXNDb250ZW50IjpbImV4cG9ydCBkZWZhdWx0IChvYmplY3Q6IG9iamVjdCk6IHN5bWJvbFtdID0+IE9iamVjdFxuICAuZ2V0T3duUHJvcGVydHlTeW1ib2xzKG9iamVjdClcbiAgLmZpbHRlcigoa2V5U3ltYm9sKTogYm9vbGVhbiA9PiBvYmplY3QucHJvcGVydHlJc0VudW1lcmFibGUoa2V5U3ltYm9sKSlcbiJdfQ==

/***/ }),

/***/ 752:
/***/ (function(module, __unusedexports, __webpack_require__) {

/* MIT license */
var cssKeywords = __webpack_require__(96);

// NOTE: conversions should only return primitive values (i.e. arrays, or
//       values that give correct `typeof` results).
//       do not use box values types (i.e. Number(), String(), etc.)

var reverseKeywords = {};
for (var key in cssKeywords) {
	if (cssKeywords.hasOwnProperty(key)) {
		reverseKeywords[cssKeywords[key]] = key;
	}
}

var convert = module.exports = {
	rgb: {channels: 3, labels: 'rgb'},
	hsl: {channels: 3, labels: 'hsl'},
	hsv: {channels: 3, labels: 'hsv'},
	hwb: {channels: 3, labels: 'hwb'},
	cmyk: {channels: 4, labels: 'cmyk'},
	xyz: {channels: 3, labels: 'xyz'},
	lab: {channels: 3, labels: 'lab'},
	lch: {channels: 3, labels: 'lch'},
	hex: {channels: 1, labels: ['hex']},
	keyword: {channels: 1, labels: ['keyword']},
	ansi16: {channels: 1, labels: ['ansi16']},
	ansi256: {channels: 1, labels: ['ansi256']},
	hcg: {channels: 3, labels: ['h', 'c', 'g']},
	apple: {channels: 3, labels: ['r16', 'g16', 'b16']},
	gray: {channels: 1, labels: ['gray']}
};

// hide .channels and .labels properties
for (var model in convert) {
	if (convert.hasOwnProperty(model)) {
		if (!('channels' in convert[model])) {
			throw new Error('missing channels property: ' + model);
		}

		if (!('labels' in convert[model])) {
			throw new Error('missing channel labels property: ' + model);
		}

		if (convert[model].labels.length !== convert[model].channels) {
			throw new Error('channel and label counts mismatch: ' + model);
		}

		var channels = convert[model].channels;
		var labels = convert[model].labels;
		delete convert[model].channels;
		delete convert[model].labels;
		Object.defineProperty(convert[model], 'channels', {value: channels});
		Object.defineProperty(convert[model], 'labels', {value: labels});
	}
}

convert.rgb.hsl = function (rgb) {
	var r = rgb[0] / 255;
	var g = rgb[1] / 255;
	var b = rgb[2] / 255;
	var min = Math.min(r, g, b);
	var max = Math.max(r, g, b);
	var delta = max - min;
	var h;
	var s;
	var l;

	if (max === min) {
		h = 0;
	} else if (r === max) {
		h = (g - b) / delta;
	} else if (g === max) {
		h = 2 + (b - r) / delta;
	} else if (b === max) {
		h = 4 + (r - g) / delta;
	}

	h = Math.min(h * 60, 360);

	if (h < 0) {
		h += 360;
	}

	l = (min + max) / 2;

	if (max === min) {
		s = 0;
	} else if (l <= 0.5) {
		s = delta / (max + min);
	} else {
		s = delta / (2 - max - min);
	}

	return [h, s * 100, l * 100];
};

convert.rgb.hsv = function (rgb) {
	var rdif;
	var gdif;
	var bdif;
	var h;
	var s;

	var r = rgb[0] / 255;
	var g = rgb[1] / 255;
	var b = rgb[2] / 255;
	var v = Math.max(r, g, b);
	var diff = v - Math.min(r, g, b);
	var diffc = function (c) {
		return (v - c) / 6 / diff + 1 / 2;
	};

	if (diff === 0) {
		h = s = 0;
	} else {
		s = diff / v;
		rdif = diffc(r);
		gdif = diffc(g);
		bdif = diffc(b);

		if (r === v) {
			h = bdif - gdif;
		} else if (g === v) {
			h = (1 / 3) + rdif - bdif;
		} else if (b === v) {
			h = (2 / 3) + gdif - rdif;
		}
		if (h < 0) {
			h += 1;
		} else if (h > 1) {
			h -= 1;
		}
	}

	return [
		h * 360,
		s * 100,
		v * 100
	];
};

convert.rgb.hwb = function (rgb) {
	var r = rgb[0];
	var g = rgb[1];
	var b = rgb[2];
	var h = convert.rgb.hsl(rgb)[0];
	var w = 1 / 255 * Math.min(r, Math.min(g, b));

	b = 1 - 1 / 255 * Math.max(r, Math.max(g, b));

	return [h, w * 100, b * 100];
};

convert.rgb.cmyk = function (rgb) {
	var r = rgb[0] / 255;
	var g = rgb[1] / 255;
	var b = rgb[2] / 255;
	var c;
	var m;
	var y;
	var k;

	k = Math.min(1 - r, 1 - g, 1 - b);
	c = (1 - r - k) / (1 - k) || 0;
	m = (1 - g - k) / (1 - k) || 0;
	y = (1 - b - k) / (1 - k) || 0;

	return [c * 100, m * 100, y * 100, k * 100];
};

/**
 * See https://en.m.wikipedia.org/wiki/Euclidean_distance#Squared_Euclidean_distance
 * */
function comparativeDistance(x, y) {
	return (
		Math.pow(x[0] - y[0], 2) +
		Math.pow(x[1] - y[1], 2) +
		Math.pow(x[2] - y[2], 2)
	);
}

convert.rgb.keyword = function (rgb) {
	var reversed = reverseKeywords[rgb];
	if (reversed) {
		return reversed;
	}

	var currentClosestDistance = Infinity;
	var currentClosestKeyword;

	for (var keyword in cssKeywords) {
		if (cssKeywords.hasOwnProperty(keyword)) {
			var value = cssKeywords[keyword];

			// Compute comparative distance
			var distance = comparativeDistance(rgb, value);

			// Check if its less, if so set as closest
			if (distance < currentClosestDistance) {
				currentClosestDistance = distance;
				currentClosestKeyword = keyword;
			}
		}
	}

	return currentClosestKeyword;
};

convert.keyword.rgb = function (keyword) {
	return cssKeywords[keyword];
};

convert.rgb.xyz = function (rgb) {
	var r = rgb[0] / 255;
	var g = rgb[1] / 255;
	var b = rgb[2] / 255;

	// assume sRGB
	r = r > 0.04045 ? Math.pow(((r + 0.055) / 1.055), 2.4) : (r / 12.92);
	g = g > 0.04045 ? Math.pow(((g + 0.055) / 1.055), 2.4) : (g / 12.92);
	b = b > 0.04045 ? Math.pow(((b + 0.055) / 1.055), 2.4) : (b / 12.92);

	var x = (r * 0.4124) + (g * 0.3576) + (b * 0.1805);
	var y = (r * 0.2126) + (g * 0.7152) + (b * 0.0722);
	var z = (r * 0.0193) + (g * 0.1192) + (b * 0.9505);

	return [x * 100, y * 100, z * 100];
};

convert.rgb.lab = function (rgb) {
	var xyz = convert.rgb.xyz(rgb);
	var x = xyz[0];
	var y = xyz[1];
	var z = xyz[2];
	var l;
	var a;
	var b;

	x /= 95.047;
	y /= 100;
	z /= 108.883;

	x = x > 0.008856 ? Math.pow(x, 1 / 3) : (7.787 * x) + (16 / 116);
	y = y > 0.008856 ? Math.pow(y, 1 / 3) : (7.787 * y) + (16 / 116);
	z = z > 0.008856 ? Math.pow(z, 1 / 3) : (7.787 * z) + (16 / 116);

	l = (116 * y) - 16;
	a = 500 * (x - y);
	b = 200 * (y - z);

	return [l, a, b];
};

convert.hsl.rgb = function (hsl) {
	var h = hsl[0] / 360;
	var s = hsl[1] / 100;
	var l = hsl[2] / 100;
	var t1;
	var t2;
	var t3;
	var rgb;
	var val;

	if (s === 0) {
		val = l * 255;
		return [val, val, val];
	}

	if (l < 0.5) {
		t2 = l * (1 + s);
	} else {
		t2 = l + s - l * s;
	}

	t1 = 2 * l - t2;

	rgb = [0, 0, 0];
	for (var i = 0; i < 3; i++) {
		t3 = h + 1 / 3 * -(i - 1);
		if (t3 < 0) {
			t3++;
		}
		if (t3 > 1) {
			t3--;
		}

		if (6 * t3 < 1) {
			val = t1 + (t2 - t1) * 6 * t3;
		} else if (2 * t3 < 1) {
			val = t2;
		} else if (3 * t3 < 2) {
			val = t1 + (t2 - t1) * (2 / 3 - t3) * 6;
		} else {
			val = t1;
		}

		rgb[i] = val * 255;
	}

	return rgb;
};

convert.hsl.hsv = function (hsl) {
	var h = hsl[0];
	var s = hsl[1] / 100;
	var l = hsl[2] / 100;
	var smin = s;
	var lmin = Math.max(l, 0.01);
	var sv;
	var v;

	l *= 2;
	s *= (l <= 1) ? l : 2 - l;
	smin *= lmin <= 1 ? lmin : 2 - lmin;
	v = (l + s) / 2;
	sv = l === 0 ? (2 * smin) / (lmin + smin) : (2 * s) / (l + s);

	return [h, sv * 100, v * 100];
};

convert.hsv.rgb = function (hsv) {
	var h = hsv[0] / 60;
	var s = hsv[1] / 100;
	var v = hsv[2] / 100;
	var hi = Math.floor(h) % 6;

	var f = h - Math.floor(h);
	var p = 255 * v * (1 - s);
	var q = 255 * v * (1 - (s * f));
	var t = 255 * v * (1 - (s * (1 - f)));
	v *= 255;

	switch (hi) {
		case 0:
			return [v, t, p];
		case 1:
			return [q, v, p];
		case 2:
			return [p, v, t];
		case 3:
			return [p, q, v];
		case 4:
			return [t, p, v];
		case 5:
			return [v, p, q];
	}
};

convert.hsv.hsl = function (hsv) {
	var h = hsv[0];
	var s = hsv[1] / 100;
	var v = hsv[2] / 100;
	var vmin = Math.max(v, 0.01);
	var lmin;
	var sl;
	var l;

	l = (2 - s) * v;
	lmin = (2 - s) * vmin;
	sl = s * vmin;
	sl /= (lmin <= 1) ? lmin : 2 - lmin;
	sl = sl || 0;
	l /= 2;

	return [h, sl * 100, l * 100];
};

// http://dev.w3.org/csswg/css-color/#hwb-to-rgb
convert.hwb.rgb = function (hwb) {
	var h = hwb[0] / 360;
	var wh = hwb[1] / 100;
	var bl = hwb[2] / 100;
	var ratio = wh + bl;
	var i;
	var v;
	var f;
	var n;

	// wh + bl cant be > 1
	if (ratio > 1) {
		wh /= ratio;
		bl /= ratio;
	}

	i = Math.floor(6 * h);
	v = 1 - bl;
	f = 6 * h - i;

	if ((i & 0x01) !== 0) {
		f = 1 - f;
	}

	n = wh + f * (v - wh); // linear interpolation

	var r;
	var g;
	var b;
	switch (i) {
		default:
		case 6:
		case 0: r = v; g = n; b = wh; break;
		case 1: r = n; g = v; b = wh; break;
		case 2: r = wh; g = v; b = n; break;
		case 3: r = wh; g = n; b = v; break;
		case 4: r = n; g = wh; b = v; break;
		case 5: r = v; g = wh; b = n; break;
	}

	return [r * 255, g * 255, b * 255];
};

convert.cmyk.rgb = function (cmyk) {
	var c = cmyk[0] / 100;
	var m = cmyk[1] / 100;
	var y = cmyk[2] / 100;
	var k = cmyk[3] / 100;
	var r;
	var g;
	var b;

	r = 1 - Math.min(1, c * (1 - k) + k);
	g = 1 - Math.min(1, m * (1 - k) + k);
	b = 1 - Math.min(1, y * (1 - k) + k);

	return [r * 255, g * 255, b * 255];
};

convert.xyz.rgb = function (xyz) {
	var x = xyz[0] / 100;
	var y = xyz[1] / 100;
	var z = xyz[2] / 100;
	var r;
	var g;
	var b;

	r = (x * 3.2406) + (y * -1.5372) + (z * -0.4986);
	g = (x * -0.9689) + (y * 1.8758) + (z * 0.0415);
	b = (x * 0.0557) + (y * -0.2040) + (z * 1.0570);

	// assume sRGB
	r = r > 0.0031308
		? ((1.055 * Math.pow(r, 1.0 / 2.4)) - 0.055)
		: r * 12.92;

	g = g > 0.0031308
		? ((1.055 * Math.pow(g, 1.0 / 2.4)) - 0.055)
		: g * 12.92;

	b = b > 0.0031308
		? ((1.055 * Math.pow(b, 1.0 / 2.4)) - 0.055)
		: b * 12.92;

	r = Math.min(Math.max(0, r), 1);
	g = Math.min(Math.max(0, g), 1);
	b = Math.min(Math.max(0, b), 1);

	return [r * 255, g * 255, b * 255];
};

convert.xyz.lab = function (xyz) {
	var x = xyz[0];
	var y = xyz[1];
	var z = xyz[2];
	var l;
	var a;
	var b;

	x /= 95.047;
	y /= 100;
	z /= 108.883;

	x = x > 0.008856 ? Math.pow(x, 1 / 3) : (7.787 * x) + (16 / 116);
	y = y > 0.008856 ? Math.pow(y, 1 / 3) : (7.787 * y) + (16 / 116);
	z = z > 0.008856 ? Math.pow(z, 1 / 3) : (7.787 * z) + (16 / 116);

	l = (116 * y) - 16;
	a = 500 * (x - y);
	b = 200 * (y - z);

	return [l, a, b];
};

convert.lab.xyz = function (lab) {
	var l = lab[0];
	var a = lab[1];
	var b = lab[2];
	var x;
	var y;
	var z;

	y = (l + 16) / 116;
	x = a / 500 + y;
	z = y - b / 200;

	var y2 = Math.pow(y, 3);
	var x2 = Math.pow(x, 3);
	var z2 = Math.pow(z, 3);
	y = y2 > 0.008856 ? y2 : (y - 16 / 116) / 7.787;
	x = x2 > 0.008856 ? x2 : (x - 16 / 116) / 7.787;
	z = z2 > 0.008856 ? z2 : (z - 16 / 116) / 7.787;

	x *= 95.047;
	y *= 100;
	z *= 108.883;

	return [x, y, z];
};

convert.lab.lch = function (lab) {
	var l = lab[0];
	var a = lab[1];
	var b = lab[2];
	var hr;
	var h;
	var c;

	hr = Math.atan2(b, a);
	h = hr * 360 / 2 / Math.PI;

	if (h < 0) {
		h += 360;
	}

	c = Math.sqrt(a * a + b * b);

	return [l, c, h];
};

convert.lch.lab = function (lch) {
	var l = lch[0];
	var c = lch[1];
	var h = lch[2];
	var a;
	var b;
	var hr;

	hr = h / 360 * 2 * Math.PI;
	a = c * Math.cos(hr);
	b = c * Math.sin(hr);

	return [l, a, b];
};

convert.rgb.ansi16 = function (args) {
	var r = args[0];
	var g = args[1];
	var b = args[2];
	var value = 1 in arguments ? arguments[1] : convert.rgb.hsv(args)[2]; // hsv -> ansi16 optimization

	value = Math.round(value / 50);

	if (value === 0) {
		return 30;
	}

	var ansi = 30
		+ ((Math.round(b / 255) << 2)
		| (Math.round(g / 255) << 1)
		| Math.round(r / 255));

	if (value === 2) {
		ansi += 60;
	}

	return ansi;
};

convert.hsv.ansi16 = function (args) {
	// optimization here; we already know the value and don't need to get
	// it converted for us.
	return convert.rgb.ansi16(convert.hsv.rgb(args), args[2]);
};

convert.rgb.ansi256 = function (args) {
	var r = args[0];
	var g = args[1];
	var b = args[2];

	// we use the extended greyscale palette here, with the exception of
	// black and white. normal palette only has 4 greyscale shades.
	if (r === g && g === b) {
		if (r < 8) {
			return 16;
		}

		if (r > 248) {
			return 231;
		}

		return Math.round(((r - 8) / 247) * 24) + 232;
	}

	var ansi = 16
		+ (36 * Math.round(r / 255 * 5))
		+ (6 * Math.round(g / 255 * 5))
		+ Math.round(b / 255 * 5);

	return ansi;
};

convert.ansi16.rgb = function (args) {
	var color = args % 10;

	// handle greyscale
	if (color === 0 || color === 7) {
		if (args > 50) {
			color += 3.5;
		}

		color = color / 10.5 * 255;

		return [color, color, color];
	}

	var mult = (~~(args > 50) + 1) * 0.5;
	var r = ((color & 1) * mult) * 255;
	var g = (((color >> 1) & 1) * mult) * 255;
	var b = (((color >> 2) & 1) * mult) * 255;

	return [r, g, b];
};

convert.ansi256.rgb = function (args) {
	// handle greyscale
	if (args >= 232) {
		var c = (args - 232) * 10 + 8;
		return [c, c, c];
	}

	args -= 16;

	var rem;
	var r = Math.floor(args / 36) / 5 * 255;
	var g = Math.floor((rem = args % 36) / 6) / 5 * 255;
	var b = (rem % 6) / 5 * 255;

	return [r, g, b];
};

convert.rgb.hex = function (args) {
	var integer = ((Math.round(args[0]) & 0xFF) << 16)
		+ ((Math.round(args[1]) & 0xFF) << 8)
		+ (Math.round(args[2]) & 0xFF);

	var string = integer.toString(16).toUpperCase();
	return '000000'.substring(string.length) + string;
};

convert.hex.rgb = function (args) {
	var match = args.toString(16).match(/[a-f0-9]{6}|[a-f0-9]{3}/i);
	if (!match) {
		return [0, 0, 0];
	}

	var colorString = match[0];

	if (match[0].length === 3) {
		colorString = colorString.split('').map(function (char) {
			return char + char;
		}).join('');
	}

	var integer = parseInt(colorString, 16);
	var r = (integer >> 16) & 0xFF;
	var g = (integer >> 8) & 0xFF;
	var b = integer & 0xFF;

	return [r, g, b];
};

convert.rgb.hcg = function (rgb) {
	var r = rgb[0] / 255;
	var g = rgb[1] / 255;
	var b = rgb[2] / 255;
	var max = Math.max(Math.max(r, g), b);
	var min = Math.min(Math.min(r, g), b);
	var chroma = (max - min);
	var grayscale;
	var hue;

	if (chroma < 1) {
		grayscale = min / (1 - chroma);
	} else {
		grayscale = 0;
	}

	if (chroma <= 0) {
		hue = 0;
	} else
	if (max === r) {
		hue = ((g - b) / chroma) % 6;
	} else
	if (max === g) {
		hue = 2 + (b - r) / chroma;
	} else {
		hue = 4 + (r - g) / chroma + 4;
	}

	hue /= 6;
	hue %= 1;

	return [hue * 360, chroma * 100, grayscale * 100];
};

convert.hsl.hcg = function (hsl) {
	var s = hsl[1] / 100;
	var l = hsl[2] / 100;
	var c = 1;
	var f = 0;

	if (l < 0.5) {
		c = 2.0 * s * l;
	} else {
		c = 2.0 * s * (1.0 - l);
	}

	if (c < 1.0) {
		f = (l - 0.5 * c) / (1.0 - c);
	}

	return [hsl[0], c * 100, f * 100];
};

convert.hsv.hcg = function (hsv) {
	var s = hsv[1] / 100;
	var v = hsv[2] / 100;

	var c = s * v;
	var f = 0;

	if (c < 1.0) {
		f = (v - c) / (1 - c);
	}

	return [hsv[0], c * 100, f * 100];
};

convert.hcg.rgb = function (hcg) {
	var h = hcg[0] / 360;
	var c = hcg[1] / 100;
	var g = hcg[2] / 100;

	if (c === 0.0) {
		return [g * 255, g * 255, g * 255];
	}

	var pure = [0, 0, 0];
	var hi = (h % 1) * 6;
	var v = hi % 1;
	var w = 1 - v;
	var mg = 0;

	switch (Math.floor(hi)) {
		case 0:
			pure[0] = 1; pure[1] = v; pure[2] = 0; break;
		case 1:
			pure[0] = w; pure[1] = 1; pure[2] = 0; break;
		case 2:
			pure[0] = 0; pure[1] = 1; pure[2] = v; break;
		case 3:
			pure[0] = 0; pure[1] = w; pure[2] = 1; break;
		case 4:
			pure[0] = v; pure[1] = 0; pure[2] = 1; break;
		default:
			pure[0] = 1; pure[1] = 0; pure[2] = w;
	}

	mg = (1.0 - c) * g;

	return [
		(c * pure[0] + mg) * 255,
		(c * pure[1] + mg) * 255,
		(c * pure[2] + mg) * 255
	];
};

convert.hcg.hsv = function (hcg) {
	var c = hcg[1] / 100;
	var g = hcg[2] / 100;

	var v = c + g * (1.0 - c);
	var f = 0;

	if (v > 0.0) {
		f = c / v;
	}

	return [hcg[0], f * 100, v * 100];
};

convert.hcg.hsl = function (hcg) {
	var c = hcg[1] / 100;
	var g = hcg[2] / 100;

	var l = g * (1.0 - c) + 0.5 * c;
	var s = 0;

	if (l > 0.0 && l < 0.5) {
		s = c / (2 * l);
	} else
	if (l >= 0.5 && l < 1.0) {
		s = c / (2 * (1 - l));
	}

	return [hcg[0], s * 100, l * 100];
};

convert.hcg.hwb = function (hcg) {
	var c = hcg[1] / 100;
	var g = hcg[2] / 100;
	var v = c + g * (1.0 - c);
	return [hcg[0], (v - c) * 100, (1 - v) * 100];
};

convert.hwb.hcg = function (hwb) {
	var w = hwb[1] / 100;
	var b = hwb[2] / 100;
	var v = 1 - b;
	var c = v - w;
	var g = 0;

	if (c < 1) {
		g = (v - c) / (1 - c);
	}

	return [hwb[0], c * 100, g * 100];
};

convert.apple.rgb = function (apple) {
	return [(apple[0] / 65535) * 255, (apple[1] / 65535) * 255, (apple[2] / 65535) * 255];
};

convert.rgb.apple = function (rgb) {
	return [(rgb[0] / 255) * 65535, (rgb[1] / 255) * 65535, (rgb[2] / 255) * 65535];
};

convert.gray.rgb = function (args) {
	return [args[0] / 100 * 255, args[0] / 100 * 255, args[0] / 100 * 255];
};

convert.gray.hsl = convert.gray.hsv = function (args) {
	return [0, 0, args[0]];
};

convert.gray.hwb = function (gray) {
	return [0, 100, gray[0]];
};

convert.gray.cmyk = function (gray) {
	return [0, 0, 0, gray[0]];
};

convert.gray.lab = function (gray) {
	return [gray[0], 0, 0];
};

convert.gray.hex = function (gray) {
	var val = Math.round(gray[0] / 100 * 255) & 0xFF;
	var integer = (val << 16) + (val << 8) + val;

	var string = integer.toString(16).toUpperCase();
	return '000000'.substring(string.length) + string;
};

convert.rgb.gray = function (rgb) {
	var val = (rgb[0] + rgb[1] + rgb[2]) / 3;
	return [val / 255 * 100];
};


/***/ }),

/***/ 763:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
const isRegexp = __webpack_require__(321);
const isObj = __webpack_require__(787);
const getOwnEnumPropSymbols = __webpack_require__(717).default;
// Fork of https://github.com/yeoman/stringify-object/blob/master/index.js
// with possibility to overwrite the whole key-value pair (options.transformLine)
/* tslint:disable */
const stringifyObject = (input, options, pad) => {
    const seen = [];
    return (function stringifyObject(input, options = {}, pad = '', path = []) {
        options.indent = options.indent || '\t';
        let tokens;
        if (options.inlineCharacterLimit === undefined) {
            tokens = {
                newLine: '\n',
                newLineOrSpace: '\n',
                pad,
                indent: pad + options.indent,
            };
        }
        else {
            tokens = {
                newLine: '@@__STRINGIFY_OBJECT_NEW_LINE__@@',
                newLineOrSpace: '@@__STRINGIFY_OBJECT_NEW_LINE_OR_SPACE__@@',
                pad: '@@__STRINGIFY_OBJECT_PAD__@@',
                indent: '@@__STRINGIFY_OBJECT_INDENT__@@',
            };
        }
        const expandWhiteSpace = string => {
            if (options.inlineCharacterLimit === undefined) {
                return string;
            }
            const oneLined = string
                .replace(new RegExp(tokens.newLine, 'g'), '')
                .replace(new RegExp(tokens.newLineOrSpace, 'g'), ' ')
                .replace(new RegExp(tokens.pad + '|' + tokens.indent, 'g'), '');
            if (oneLined.length <= options.inlineCharacterLimit) {
                return oneLined;
            }
            return string
                .replace(new RegExp(tokens.newLine + '|' + tokens.newLineOrSpace, 'g'), '\n')
                .replace(new RegExp(tokens.pad, 'g'), pad)
                .replace(new RegExp(tokens.indent, 'g'), pad + options.indent);
        };
        if (seen.indexOf(input) !== -1) {
            return '"[Circular]"';
        }
        if (input === null ||
            input === undefined ||
            typeof input === 'number' ||
            typeof input === 'boolean' ||
            typeof input === 'function' ||
            typeof input === 'symbol' ||
            isRegexp(input)) {
            return String(input);
        }
        if (input instanceof Date) {
            return `new Date('${input.toISOString()}')`;
        }
        if (Array.isArray(input)) {
            if (input.length === 0) {
                return '[]';
            }
            seen.push(input);
            const ret = '[' +
                tokens.newLine +
                input
                    .map((el, i) => {
                    const eol = input.length - 1 === i ? tokens.newLine : ',' + tokens.newLineOrSpace;
                    let value = stringifyObject(el, options, pad + options.indent, [...path, i]);
                    if (options.transformValue) {
                        value = options.transformValue(input, i, value);
                    }
                    return tokens.indent + value + eol;
                })
                    .join('') +
                tokens.pad +
                ']';
            seen.pop();
            return expandWhiteSpace(ret);
        }
        if (isObj(input)) {
            let objKeys = Object.keys(input).concat(getOwnEnumPropSymbols(input));
            if (options.filter) {
                objKeys = objKeys.filter(el => options.filter(input, el));
            }
            if (objKeys.length === 0) {
                return '{}';
            }
            seen.push(input);
            const ret = '{' +
                tokens.newLine +
                objKeys
                    .map((el, i) => {
                    const eol = objKeys.length - 1 === i ? tokens.newLine : ',' + tokens.newLineOrSpace;
                    const isSymbol = typeof el === 'symbol';
                    const isClassic = !isSymbol && /^[a-z$_][a-z$_0-9]*$/i.test(el);
                    const key = isSymbol || isClassic ? el : stringifyObject(el, options, undefined, [...path, el]);
                    let value = stringifyObject(input[el], options, pad + options.indent, [...path, el]);
                    if (options.transformValue) {
                        value = options.transformValue(input, el, value);
                    }
                    let line = tokens.indent + String(key) + ': ' + value + eol;
                    if (options.transformLine) {
                        line = options.transformLine({
                            obj: input,
                            indent: tokens.indent,
                            key,
                            stringifiedValue: value,
                            value: input[el],
                            eol,
                            originalLine: line,
                            path: path.concat(key),
                        });
                    }
                    return line;
                })
                    .join('') +
                tokens.pad +
                '}';
            seen.pop();
            return expandWhiteSpace(ret);
        }
        input = String(input).replace(/[\r\n]/g, x => (x === '\n' ? '\\n' : '\\r'));
        if (options.singleQuotes === false) {
            input = input.replace(/"/g, '\\"');
            return `"${input}"`;
        }
        input = input.replace(/\\?'/g, "\\'");
        return `'${input}'`;
    })(input, options, pad);
};
exports.default = stringifyObject;


/***/ }),

/***/ 772:
/***/ (function(__unusedmodule, exports) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
function omit(object, path) {
    const result = {};
    for (const key in object) {
        if (key !== path) {
            // TypeScript bug
            result[key] = object[key];
        }
    }
    return result;
}
exports.omit = omit;


/***/ }),

/***/ 787:
/***/ (function(module) {

"use strict";


module.exports = value => {
	const type = typeof value;
	return value !== null && (type === 'object' || type === 'function');
};


/***/ }),

/***/ 791:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
var dmmf_types_1 = __webpack_require__(863);
exports.DMMF = dmmf_types_1.DMMF;
var dmmf_1 = __webpack_require__(587);
exports.DMMFClass = dmmf_1.DMMFClass;
var deep_set_1 = __webpack_require__(953);
exports.deepGet = deep_set_1.deepGet;
exports.deepSet = deep_set_1.deepSet;
var query_1 = __webpack_require__(457);
exports.makeDocument = query_1.makeDocument;
exports.transformDocument = query_1.transformDocument;
var NodeEngine_1 = __webpack_require__(141);
exports.Engine = NodeEngine_1.NodeEngine;
var debug_1 = __webpack_require__(79);
exports.debugLib = debug_1.default;


/***/ }),

/***/ 805:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
const query_1 = __webpack_require__(457);
function visit(document, visitor) {
    const children = document.children.map(field => visitField(field, visitor));
    if (document.children.length === children.length &&
        document.children.every((child, index) => child === children[index])) {
        return document;
    }
    const newDoc = new query_1.Document(document.type, children);
    return newDoc;
}
exports.visit = visit;
function visitField(field, visitor) {
    const args = field.args ? field.args.args.map(arg => visitArg(arg, visitor)) : undefined;
    const newArgs = args ? new query_1.Args(args) : undefined;
    const children = field.children ? field.children.map(child => visitField(child, visitor)) : undefined;
    const argsDidntChange = (!newArgs && !field.args) ||
        (field.args &&
            newArgs &&
            (field.args.args.length === newArgs.args.length &&
                field.args.args.every((arg, index) => arg === newArgs.args[index])));
    const fieldsDidntChange = (!field.children && !children) ||
        (field.children &&
            children &&
            field.children.length === children.length &&
            field.children.every((child, index) => child === children[index]));
    if (argsDidntChange && fieldsDidntChange) {
        return field;
    }
    return new query_1.Field({
        name: field.name,
        args: newArgs,
        children,
        error: field.error,
    });
}
function isArgsArray(input) {
    if (Array.isArray(input)) {
        return input.every(arg => arg instanceof query_1.Args);
    }
    return false;
}
function visitArg(arg, visitor) {
    function mapArgs(inputArgs) {
        const { args } = inputArgs;
        const newArgs = args.map(a => visitArg(a, visitor));
        if (newArgs.length !== args.length || args.find((a, i) => a !== newArgs[i])) {
            return new query_1.Args(newArgs);
        }
        return inputArgs;
    }
    const newArg = visitor.Arg.enter(arg) || arg;
    let newValue = newArg.value;
    if (isArgsArray(newArg.value)) {
        newValue = newArg.value.map(mapArgs);
    }
    else if (newArg.value instanceof query_1.Args) {
        newValue = mapArgs(newArg.value);
    }
    if (newValue !== newArg.value) {
        return new query_1.Arg({
            key: newArg.key,
            value: newValue,
            error: newArg.error,
            argType: newArg.argType,
            isEnum: newArg.isEnum,
        });
    }
    return newArg;
}


/***/ }),

/***/ 816:
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var Stream = _interopDefault(__webpack_require__(688));
var http = _interopDefault(__webpack_require__(421));
var Url = _interopDefault(__webpack_require__(77));
var https = _interopDefault(__webpack_require__(230));
var zlib = _interopDefault(__webpack_require__(267));

// Based on https://github.com/tmpvar/jsdom/blob/aa85b2abf07766ff7bf5c1f6daafb3726f2f2db5/lib/jsdom/living/blob.js
// (MIT licensed)

const BUFFER = Symbol('buffer');
const TYPE = Symbol('type');

class Blob {
	constructor() {
		this[TYPE] = '';

		const blobParts = arguments[0];
		const options = arguments[1];

		const buffers = [];

		if (blobParts) {
			const a = blobParts;
			const length = Number(a.length);
			for (let i = 0; i < length; i++) {
				const element = a[i];
				let buffer;
				if (element instanceof Buffer) {
					buffer = element;
				} else if (ArrayBuffer.isView(element)) {
					buffer = Buffer.from(element.buffer, element.byteOffset, element.byteLength);
				} else if (element instanceof ArrayBuffer) {
					buffer = Buffer.from(element);
				} else if (element instanceof Blob) {
					buffer = element[BUFFER];
				} else {
					buffer = Buffer.from(typeof element === 'string' ? element : String(element));
				}
				buffers.push(buffer);
			}
		}

		this[BUFFER] = Buffer.concat(buffers);

		let type = options && options.type !== undefined && String(options.type).toLowerCase();
		if (type && !/[^\u0020-\u007E]/.test(type)) {
			this[TYPE] = type;
		}
	}
	get size() {
		return this[BUFFER].length;
	}
	get type() {
		return this[TYPE];
	}
	slice() {
		const size = this.size;

		const start = arguments[0];
		const end = arguments[1];
		let relativeStart, relativeEnd;
		if (start === undefined) {
			relativeStart = 0;
		} else if (start < 0) {
			relativeStart = Math.max(size + start, 0);
		} else {
			relativeStart = Math.min(start, size);
		}
		if (end === undefined) {
			relativeEnd = size;
		} else if (end < 0) {
			relativeEnd = Math.max(size + end, 0);
		} else {
			relativeEnd = Math.min(end, size);
		}
		const span = Math.max(relativeEnd - relativeStart, 0);

		const buffer = this[BUFFER];
		const slicedBuffer = buffer.slice(relativeStart, relativeStart + span);
		const blob = new Blob([], { type: arguments[2] });
		blob[BUFFER] = slicedBuffer;
		return blob;
	}
}

Object.defineProperties(Blob.prototype, {
	size: { enumerable: true },
	type: { enumerable: true },
	slice: { enumerable: true }
});

Object.defineProperty(Blob.prototype, Symbol.toStringTag, {
	value: 'Blob',
	writable: false,
	enumerable: false,
	configurable: true
});

/**
 * fetch-error.js
 *
 * FetchError interface for operational errors
 */

/**
 * Create FetchError instance
 *
 * @param   String      message      Error message for human
 * @param   String      type         Error type for machine
 * @param   String      systemError  For Node.js system error
 * @return  FetchError
 */
function FetchError(message, type, systemError) {
  Error.call(this, message);

  this.message = message;
  this.type = type;

  // when err.type is `system`, err.code contains system error code
  if (systemError) {
    this.code = this.errno = systemError.code;
  }

  // hide custom error implementation details from end-users
  Error.captureStackTrace(this, this.constructor);
}

FetchError.prototype = Object.create(Error.prototype);
FetchError.prototype.constructor = FetchError;
FetchError.prototype.name = 'FetchError';

let convert;
try {
	convert = __webpack_require__(15).convert;
} catch (e) {}

const INTERNALS = Symbol('Body internals');

// fix an issue where "PassThrough" isn't a named export for node <10
const PassThrough = Stream.PassThrough;

/**
 * Body mixin
 *
 * Ref: https://fetch.spec.whatwg.org/#body
 *
 * @param   Stream  body  Readable stream
 * @param   Object  opts  Response options
 * @return  Void
 */
function Body(body) {
	var _this = this;

	var _ref = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
	    _ref$size = _ref.size;

	let size = _ref$size === undefined ? 0 : _ref$size;
	var _ref$timeout = _ref.timeout;
	let timeout = _ref$timeout === undefined ? 0 : _ref$timeout;

	if (body == null) {
		// body is undefined or null
		body = null;
	} else if (typeof body === 'string') ; else if (isURLSearchParams(body)) ; else if (body instanceof Blob) ; else if (Buffer.isBuffer(body)) ; else if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') ; else if (ArrayBuffer.isView(body)) ; else if (body instanceof Stream) ; else {
		// none of the above
		// coerce to string
		body = String(body);
	}
	this[INTERNALS] = {
		body,
		disturbed: false,
		error: null
	};
	this.size = size;
	this.timeout = timeout;

	if (body instanceof Stream) {
		body.on('error', function (err) {
			const error = err.name === 'AbortError' ? err : new FetchError(`Invalid response body while trying to fetch ${_this.url}: ${err.message}`, 'system', err);
			_this[INTERNALS].error = error;
		});
	}
}

Body.prototype = {
	get body() {
		return this[INTERNALS].body;
	},

	get bodyUsed() {
		return this[INTERNALS].disturbed;
	},

	/**
  * Decode response as ArrayBuffer
  *
  * @return  Promise
  */
	arrayBuffer() {
		return consumeBody.call(this).then(function (buf) {
			return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
		});
	},

	/**
  * Return raw response as Blob
  *
  * @return Promise
  */
	blob() {
		let ct = this.headers && this.headers.get('content-type') || '';
		return consumeBody.call(this).then(function (buf) {
			return Object.assign(
			// Prevent copying
			new Blob([], {
				type: ct.toLowerCase()
			}), {
				[BUFFER]: buf
			});
		});
	},

	/**
  * Decode response as json
  *
  * @return  Promise
  */
	json() {
		var _this2 = this;

		return consumeBody.call(this).then(function (buffer) {
			try {
				return JSON.parse(buffer.toString());
			} catch (err) {
				return Body.Promise.reject(new FetchError(`invalid json response body at ${_this2.url} reason: ${err.message}`, 'invalid-json'));
			}
		});
	},

	/**
  * Decode response as text
  *
  * @return  Promise
  */
	text() {
		return consumeBody.call(this).then(function (buffer) {
			return buffer.toString();
		});
	},

	/**
  * Decode response as buffer (non-spec api)
  *
  * @return  Promise
  */
	buffer() {
		return consumeBody.call(this);
	},

	/**
  * Decode response as text, while automatically detecting the encoding and
  * trying to decode to UTF-8 (non-spec api)
  *
  * @return  Promise
  */
	textConverted() {
		var _this3 = this;

		return consumeBody.call(this).then(function (buffer) {
			return convertBody(buffer, _this3.headers);
		});
	}

};

// In browsers, all properties are enumerable.
Object.defineProperties(Body.prototype, {
	body: { enumerable: true },
	bodyUsed: { enumerable: true },
	arrayBuffer: { enumerable: true },
	blob: { enumerable: true },
	json: { enumerable: true },
	text: { enumerable: true }
});

Body.mixIn = function (proto) {
	for (const name of Object.getOwnPropertyNames(Body.prototype)) {
		// istanbul ignore else: future proof
		if (!(name in proto)) {
			const desc = Object.getOwnPropertyDescriptor(Body.prototype, name);
			Object.defineProperty(proto, name, desc);
		}
	}
};

/**
 * Consume and convert an entire Body to a Buffer.
 *
 * Ref: https://fetch.spec.whatwg.org/#concept-body-consume-body
 *
 * @return  Promise
 */
function consumeBody() {
	var _this4 = this;

	if (this[INTERNALS].disturbed) {
		return Body.Promise.reject(new TypeError(`body used already for: ${this.url}`));
	}

	this[INTERNALS].disturbed = true;

	if (this[INTERNALS].error) {
		return Body.Promise.reject(this[INTERNALS].error);
	}

	// body is null
	if (this.body === null) {
		return Body.Promise.resolve(Buffer.alloc(0));
	}

	// body is string
	if (typeof this.body === 'string') {
		return Body.Promise.resolve(Buffer.from(this.body));
	}

	// body is blob
	if (this.body instanceof Blob) {
		return Body.Promise.resolve(this.body[BUFFER]);
	}

	// body is buffer
	if (Buffer.isBuffer(this.body)) {
		return Body.Promise.resolve(this.body);
	}

	// body is ArrayBuffer
	if (Object.prototype.toString.call(this.body) === '[object ArrayBuffer]') {
		return Body.Promise.resolve(Buffer.from(this.body));
	}

	// body is ArrayBufferView
	if (ArrayBuffer.isView(this.body)) {
		return Body.Promise.resolve(Buffer.from(this.body.buffer, this.body.byteOffset, this.body.byteLength));
	}

	// istanbul ignore if: should never happen
	if (!(this.body instanceof Stream)) {
		return Body.Promise.resolve(Buffer.alloc(0));
	}

	// body is stream
	// get ready to actually consume the body
	let accum = [];
	let accumBytes = 0;
	let abort = false;

	return new Body.Promise(function (resolve, reject) {
		let resTimeout;

		// allow timeout on slow response body
		if (_this4.timeout) {
			resTimeout = setTimeout(function () {
				abort = true;
				reject(new FetchError(`Response timeout while trying to fetch ${_this4.url} (over ${_this4.timeout}ms)`, 'body-timeout'));
			}, _this4.timeout);
		}

		// handle stream errors
		_this4.body.on('error', function (err) {
			if (err.name === 'AbortError') {
				// if the request was aborted, reject with this Error
				abort = true;
				reject(err);
			} else {
				// other errors, such as incorrect content-encoding
				reject(new FetchError(`Invalid response body while trying to fetch ${_this4.url}: ${err.message}`, 'system', err));
			}
		});

		_this4.body.on('data', function (chunk) {
			if (abort || chunk === null) {
				return;
			}

			if (_this4.size && accumBytes + chunk.length > _this4.size) {
				abort = true;
				reject(new FetchError(`content size at ${_this4.url} over limit: ${_this4.size}`, 'max-size'));
				return;
			}

			accumBytes += chunk.length;
			accum.push(chunk);
		});

		_this4.body.on('end', function () {
			if (abort) {
				return;
			}

			clearTimeout(resTimeout);

			try {
				resolve(Buffer.concat(accum));
			} catch (err) {
				// handle streams that have accumulated too much data (issue #414)
				reject(new FetchError(`Could not create Buffer from response body for ${_this4.url}: ${err.message}`, 'system', err));
			}
		});
	});
}

/**
 * Detect buffer encoding and convert to target encoding
 * ref: http://www.w3.org/TR/2011/WD-html5-20110113/parsing.html#determining-the-character-encoding
 *
 * @param   Buffer  buffer    Incoming buffer
 * @param   String  encoding  Target encoding
 * @return  String
 */
function convertBody(buffer, headers) {
	if (typeof convert !== 'function') {
		throw new Error('The package `encoding` must be installed to use the textConverted() function');
	}

	const ct = headers.get('content-type');
	let charset = 'utf-8';
	let res, str;

	// header
	if (ct) {
		res = /charset=([^;]*)/i.exec(ct);
	}

	// no charset in content type, peek at response body for at most 1024 bytes
	str = buffer.slice(0, 1024).toString();

	// html5
	if (!res && str) {
		res = /<meta.+?charset=(['"])(.+?)\1/i.exec(str);
	}

	// html4
	if (!res && str) {
		res = /<meta[\s]+?http-equiv=(['"])content-type\1[\s]+?content=(['"])(.+?)\2/i.exec(str);

		if (res) {
			res = /charset=(.*)/i.exec(res.pop());
		}
	}

	// xml
	if (!res && str) {
		res = /<\?xml.+?encoding=(['"])(.+?)\1/i.exec(str);
	}

	// found charset
	if (res) {
		charset = res.pop();

		// prevent decode issues when sites use incorrect encoding
		// ref: https://hsivonen.fi/encoding-menu/
		if (charset === 'gb2312' || charset === 'gbk') {
			charset = 'gb18030';
		}
	}

	// turn raw buffers into a single utf-8 buffer
	return convert(buffer, 'UTF-8', charset).toString();
}

/**
 * Detect a URLSearchParams object
 * ref: https://github.com/bitinn/node-fetch/issues/296#issuecomment-307598143
 *
 * @param   Object  obj     Object to detect by type or brand
 * @return  String
 */
function isURLSearchParams(obj) {
	// Duck-typing as a necessary condition.
	if (typeof obj !== 'object' || typeof obj.append !== 'function' || typeof obj.delete !== 'function' || typeof obj.get !== 'function' || typeof obj.getAll !== 'function' || typeof obj.has !== 'function' || typeof obj.set !== 'function') {
		return false;
	}

	// Brand-checking and more duck-typing as optional condition.
	return obj.constructor.name === 'URLSearchParams' || Object.prototype.toString.call(obj) === '[object URLSearchParams]' || typeof obj.sort === 'function';
}

/**
 * Clone body given Res/Req instance
 *
 * @param   Mixed  instance  Response or Request instance
 * @return  Mixed
 */
function clone(instance) {
	let p1, p2;
	let body = instance.body;

	// don't allow cloning a used body
	if (instance.bodyUsed) {
		throw new Error('cannot clone body after it is used');
	}

	// check that body is a stream and not form-data object
	// note: we can't clone the form-data object without having it as a dependency
	if (body instanceof Stream && typeof body.getBoundary !== 'function') {
		// tee instance body
		p1 = new PassThrough();
		p2 = new PassThrough();
		body.pipe(p1);
		body.pipe(p2);
		// set instance body to teed body and return the other teed body
		instance[INTERNALS].body = p1;
		body = p2;
	}

	return body;
}

/**
 * Performs the operation "extract a `Content-Type` value from |object|" as
 * specified in the specification:
 * https://fetch.spec.whatwg.org/#concept-bodyinit-extract
 *
 * This function assumes that instance.body is present.
 *
 * @param   Mixed  instance  Response or Request instance
 */
function extractContentType(instance) {
	const body = instance.body;

	// istanbul ignore if: Currently, because of a guard in Request, body
	// can never be null. Included here for completeness.

	if (body === null) {
		// body is null
		return null;
	} else if (typeof body === 'string') {
		// body is string
		return 'text/plain;charset=UTF-8';
	} else if (isURLSearchParams(body)) {
		// body is a URLSearchParams
		return 'application/x-www-form-urlencoded;charset=UTF-8';
	} else if (body instanceof Blob) {
		// body is blob
		return body.type || null;
	} else if (Buffer.isBuffer(body)) {
		// body is buffer
		return null;
	} else if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') {
		// body is ArrayBuffer
		return null;
	} else if (ArrayBuffer.isView(body)) {
		// body is ArrayBufferView
		return null;
	} else if (typeof body.getBoundary === 'function') {
		// detect form data input from form-data module
		return `multipart/form-data;boundary=${body.getBoundary()}`;
	} else {
		// body is stream
		// can't really do much about this
		return null;
	}
}

/**
 * The Fetch Standard treats this as if "total bytes" is a property on the body.
 * For us, we have to explicitly get it with a function.
 *
 * ref: https://fetch.spec.whatwg.org/#concept-body-total-bytes
 *
 * @param   Body    instance   Instance of Body
 * @return  Number?            Number of bytes, or null if not possible
 */
function getTotalBytes(instance) {
	const body = instance.body;

	// istanbul ignore if: included for completion

	if (body === null) {
		// body is null
		return 0;
	} else if (typeof body === 'string') {
		// body is string
		return Buffer.byteLength(body);
	} else if (isURLSearchParams(body)) {
		// body is URLSearchParams
		return Buffer.byteLength(String(body));
	} else if (body instanceof Blob) {
		// body is blob
		return body.size;
	} else if (Buffer.isBuffer(body)) {
		// body is buffer
		return body.length;
	} else if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') {
		// body is ArrayBuffer
		return body.byteLength;
	} else if (ArrayBuffer.isView(body)) {
		// body is ArrayBufferView
		return body.byteLength;
	} else if (body && typeof body.getLengthSync === 'function') {
		// detect form data input from form-data module
		if (body._lengthRetrievers && body._lengthRetrievers.length == 0 || // 1.x
		body.hasKnownLength && body.hasKnownLength()) {
			// 2.x
			return body.getLengthSync();
		}
		return null;
	} else {
		// body is stream
		// can't really do much about this
		return null;
	}
}

/**
 * Write a Body to a Node.js WritableStream (e.g. http.Request) object.
 *
 * @param   Body    instance   Instance of Body
 * @return  Void
 */
function writeToStream(dest, instance) {
	const body = instance.body;


	if (body === null) {
		// body is null
		dest.end();
	} else if (typeof body === 'string') {
		// body is string
		dest.write(body);
		dest.end();
	} else if (isURLSearchParams(body)) {
		// body is URLSearchParams
		dest.write(Buffer.from(String(body)));
		dest.end();
	} else if (body instanceof Blob) {
		// body is blob
		dest.write(body[BUFFER]);
		dest.end();
	} else if (Buffer.isBuffer(body)) {
		// body is buffer
		dest.write(body);
		dest.end();
	} else if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') {
		// body is ArrayBuffer
		dest.write(Buffer.from(body));
		dest.end();
	} else if (ArrayBuffer.isView(body)) {
		// body is ArrayBufferView
		dest.write(Buffer.from(body.buffer, body.byteOffset, body.byteLength));
		dest.end();
	} else {
		// body is stream
		body.pipe(dest);
	}
}

// expose Promise
Body.Promise = global.Promise;

/**
 * headers.js
 *
 * Headers class offers convenient helpers
 */

const invalidTokenRegex = /[^\^_`a-zA-Z\-0-9!#$%&'*+.|~]/;
const invalidHeaderCharRegex = /[^\t\x20-\x7e\x80-\xff]/;

function validateName(name) {
	name = `${name}`;
	if (invalidTokenRegex.test(name)) {
		throw new TypeError(`${name} is not a legal HTTP header name`);
	}
}

function validateValue(value) {
	value = `${value}`;
	if (invalidHeaderCharRegex.test(value)) {
		throw new TypeError(`${value} is not a legal HTTP header value`);
	}
}

/**
 * Find the key in the map object given a header name.
 *
 * Returns undefined if not found.
 *
 * @param   String  name  Header name
 * @return  String|Undefined
 */
function find(map, name) {
	name = name.toLowerCase();
	for (const key in map) {
		if (key.toLowerCase() === name) {
			return key;
		}
	}
	return undefined;
}

const MAP = Symbol('map');
class Headers {
	/**
  * Headers class
  *
  * @param   Object  headers  Response headers
  * @return  Void
  */
	constructor() {
		let init = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : undefined;

		this[MAP] = Object.create(null);

		if (init instanceof Headers) {
			const rawHeaders = init.raw();
			const headerNames = Object.keys(rawHeaders);

			for (const headerName of headerNames) {
				for (const value of rawHeaders[headerName]) {
					this.append(headerName, value);
				}
			}

			return;
		}

		// We don't worry about converting prop to ByteString here as append()
		// will handle it.
		if (init == null) ; else if (typeof init === 'object') {
			const method = init[Symbol.iterator];
			if (method != null) {
				if (typeof method !== 'function') {
					throw new TypeError('Header pairs must be iterable');
				}

				// sequence<sequence<ByteString>>
				// Note: per spec we have to first exhaust the lists then process them
				const pairs = [];
				for (const pair of init) {
					if (typeof pair !== 'object' || typeof pair[Symbol.iterator] !== 'function') {
						throw new TypeError('Each header pair must be iterable');
					}
					pairs.push(Array.from(pair));
				}

				for (const pair of pairs) {
					if (pair.length !== 2) {
						throw new TypeError('Each header pair must be a name/value tuple');
					}
					this.append(pair[0], pair[1]);
				}
			} else {
				// record<ByteString, ByteString>
				for (const key of Object.keys(init)) {
					const value = init[key];
					this.append(key, value);
				}
			}
		} else {
			throw new TypeError('Provided initializer must be an object');
		}
	}

	/**
  * Return combined header value given name
  *
  * @param   String  name  Header name
  * @return  Mixed
  */
	get(name) {
		name = `${name}`;
		validateName(name);
		const key = find(this[MAP], name);
		if (key === undefined) {
			return null;
		}

		return this[MAP][key].join(', ');
	}

	/**
  * Iterate over all headers
  *
  * @param   Function  callback  Executed for each item with parameters (value, name, thisArg)
  * @param   Boolean   thisArg   `this` context for callback function
  * @return  Void
  */
	forEach(callback) {
		let thisArg = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : undefined;

		let pairs = getHeaders(this);
		let i = 0;
		while (i < pairs.length) {
			var _pairs$i = pairs[i];
			const name = _pairs$i[0],
			      value = _pairs$i[1];

			callback.call(thisArg, value, name, this);
			pairs = getHeaders(this);
			i++;
		}
	}

	/**
  * Overwrite header values given name
  *
  * @param   String  name   Header name
  * @param   String  value  Header value
  * @return  Void
  */
	set(name, value) {
		name = `${name}`;
		value = `${value}`;
		validateName(name);
		validateValue(value);
		const key = find(this[MAP], name);
		this[MAP][key !== undefined ? key : name] = [value];
	}

	/**
  * Append a value onto existing header
  *
  * @param   String  name   Header name
  * @param   String  value  Header value
  * @return  Void
  */
	append(name, value) {
		name = `${name}`;
		value = `${value}`;
		validateName(name);
		validateValue(value);
		const key = find(this[MAP], name);
		if (key !== undefined) {
			this[MAP][key].push(value);
		} else {
			this[MAP][name] = [value];
		}
	}

	/**
  * Check for header name existence
  *
  * @param   String   name  Header name
  * @return  Boolean
  */
	has(name) {
		name = `${name}`;
		validateName(name);
		return find(this[MAP], name) !== undefined;
	}

	/**
  * Delete all header values given name
  *
  * @param   String  name  Header name
  * @return  Void
  */
	delete(name) {
		name = `${name}`;
		validateName(name);
		const key = find(this[MAP], name);
		if (key !== undefined) {
			delete this[MAP][key];
		}
	}

	/**
  * Return raw headers (non-spec api)
  *
  * @return  Object
  */
	raw() {
		return this[MAP];
	}

	/**
  * Get an iterator on keys.
  *
  * @return  Iterator
  */
	keys() {
		return createHeadersIterator(this, 'key');
	}

	/**
  * Get an iterator on values.
  *
  * @return  Iterator
  */
	values() {
		return createHeadersIterator(this, 'value');
	}

	/**
  * Get an iterator on entries.
  *
  * This is the default iterator of the Headers object.
  *
  * @return  Iterator
  */
	[Symbol.iterator]() {
		return createHeadersIterator(this, 'key+value');
	}
}
Headers.prototype.entries = Headers.prototype[Symbol.iterator];

Object.defineProperty(Headers.prototype, Symbol.toStringTag, {
	value: 'Headers',
	writable: false,
	enumerable: false,
	configurable: true
});

Object.defineProperties(Headers.prototype, {
	get: { enumerable: true },
	forEach: { enumerable: true },
	set: { enumerable: true },
	append: { enumerable: true },
	has: { enumerable: true },
	delete: { enumerable: true },
	keys: { enumerable: true },
	values: { enumerable: true },
	entries: { enumerable: true }
});

function getHeaders(headers) {
	let kind = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 'key+value';

	const keys = Object.keys(headers[MAP]).sort();
	return keys.map(kind === 'key' ? function (k) {
		return k.toLowerCase();
	} : kind === 'value' ? function (k) {
		return headers[MAP][k].join(', ');
	} : function (k) {
		return [k.toLowerCase(), headers[MAP][k].join(', ')];
	});
}

const INTERNAL = Symbol('internal');

function createHeadersIterator(target, kind) {
	const iterator = Object.create(HeadersIteratorPrototype);
	iterator[INTERNAL] = {
		target,
		kind,
		index: 0
	};
	return iterator;
}

const HeadersIteratorPrototype = Object.setPrototypeOf({
	next() {
		// istanbul ignore if
		if (!this || Object.getPrototypeOf(this) !== HeadersIteratorPrototype) {
			throw new TypeError('Value of `this` is not a HeadersIterator');
		}

		var _INTERNAL = this[INTERNAL];
		const target = _INTERNAL.target,
		      kind = _INTERNAL.kind,
		      index = _INTERNAL.index;

		const values = getHeaders(target, kind);
		const len = values.length;
		if (index >= len) {
			return {
				value: undefined,
				done: true
			};
		}

		this[INTERNAL].index = index + 1;

		return {
			value: values[index],
			done: false
		};
	}
}, Object.getPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]())));

Object.defineProperty(HeadersIteratorPrototype, Symbol.toStringTag, {
	value: 'HeadersIterator',
	writable: false,
	enumerable: false,
	configurable: true
});

/**
 * Export the Headers object in a form that Node.js can consume.
 *
 * @param   Headers  headers
 * @return  Object
 */
function exportNodeCompatibleHeaders(headers) {
	const obj = Object.assign({ __proto__: null }, headers[MAP]);

	// http.request() only supports string as Host header. This hack makes
	// specifying custom Host header possible.
	const hostHeaderKey = find(headers[MAP], 'Host');
	if (hostHeaderKey !== undefined) {
		obj[hostHeaderKey] = obj[hostHeaderKey][0];
	}

	return obj;
}

/**
 * Create a Headers object from an object of headers, ignoring those that do
 * not conform to HTTP grammar productions.
 *
 * @param   Object  obj  Object of headers
 * @return  Headers
 */
function createHeadersLenient(obj) {
	const headers = new Headers();
	for (const name of Object.keys(obj)) {
		if (invalidTokenRegex.test(name)) {
			continue;
		}
		if (Array.isArray(obj[name])) {
			for (const val of obj[name]) {
				if (invalidHeaderCharRegex.test(val)) {
					continue;
				}
				if (headers[MAP][name] === undefined) {
					headers[MAP][name] = [val];
				} else {
					headers[MAP][name].push(val);
				}
			}
		} else if (!invalidHeaderCharRegex.test(obj[name])) {
			headers[MAP][name] = [obj[name]];
		}
	}
	return headers;
}

const INTERNALS$1 = Symbol('Response internals');

// fix an issue where "STATUS_CODES" aren't a named export for node <10
const STATUS_CODES = http.STATUS_CODES;

/**
 * Response class
 *
 * @param   Stream  body  Readable stream
 * @param   Object  opts  Response options
 * @return  Void
 */
class Response {
	constructor() {
		let body = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : null;
		let opts = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

		Body.call(this, body, opts);

		const status = opts.status || 200;

		this[INTERNALS$1] = {
			url: opts.url,
			status,
			statusText: opts.statusText || STATUS_CODES[status],
			headers: new Headers(opts.headers)
		};
	}

	get url() {
		return this[INTERNALS$1].url;
	}

	get status() {
		return this[INTERNALS$1].status;
	}

	/**
  * Convenience property representing if the request ended normally
  */
	get ok() {
		return this[INTERNALS$1].status >= 200 && this[INTERNALS$1].status < 300;
	}

	get statusText() {
		return this[INTERNALS$1].statusText;
	}

	get headers() {
		return this[INTERNALS$1].headers;
	}

	/**
  * Clone this response
  *
  * @return  Response
  */
	clone() {
		return new Response(clone(this), {
			url: this.url,
			status: this.status,
			statusText: this.statusText,
			headers: this.headers,
			ok: this.ok
		});
	}
}

Body.mixIn(Response.prototype);

Object.defineProperties(Response.prototype, {
	url: { enumerable: true },
	status: { enumerable: true },
	ok: { enumerable: true },
	statusText: { enumerable: true },
	headers: { enumerable: true },
	clone: { enumerable: true }
});

Object.defineProperty(Response.prototype, Symbol.toStringTag, {
	value: 'Response',
	writable: false,
	enumerable: false,
	configurable: true
});

const INTERNALS$2 = Symbol('Request internals');

// fix an issue where "format", "parse" aren't a named export for node <10
const parse_url = Url.parse;
const format_url = Url.format;

const streamDestructionSupported = 'destroy' in Stream.Readable.prototype;

/**
 * Check if a value is an instance of Request.
 *
 * @param   Mixed   input
 * @return  Boolean
 */
function isRequest(input) {
	return typeof input === 'object' && typeof input[INTERNALS$2] === 'object';
}

function isAbortSignal(signal) {
	const proto = signal && typeof signal === 'object' && Object.getPrototypeOf(signal);
	return !!(proto && proto.constructor.name === 'AbortSignal');
}

/**
 * Request class
 *
 * @param   Mixed   input  Url or Request instance
 * @param   Object  init   Custom options
 * @return  Void
 */
class Request {
	constructor(input) {
		let init = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

		let parsedURL;

		// normalize input
		if (!isRequest(input)) {
			if (input && input.href) {
				// in order to support Node.js' Url objects; though WHATWG's URL objects
				// will fall into this branch also (since their `toString()` will return
				// `href` property anyway)
				parsedURL = parse_url(input.href);
			} else {
				// coerce input to a string before attempting to parse
				parsedURL = parse_url(`${input}`);
			}
			input = {};
		} else {
			parsedURL = parse_url(input.url);
		}

		let method = init.method || input.method || 'GET';
		method = method.toUpperCase();

		if ((init.body != null || isRequest(input) && input.body !== null) && (method === 'GET' || method === 'HEAD')) {
			throw new TypeError('Request with GET/HEAD method cannot have body');
		}

		let inputBody = init.body != null ? init.body : isRequest(input) && input.body !== null ? clone(input) : null;

		Body.call(this, inputBody, {
			timeout: init.timeout || input.timeout || 0,
			size: init.size || input.size || 0
		});

		const headers = new Headers(init.headers || input.headers || {});

		if (init.body != null) {
			const contentType = extractContentType(this);
			if (contentType !== null && !headers.has('Content-Type')) {
				headers.append('Content-Type', contentType);
			}
		}

		let signal = isRequest(input) ? input.signal : null;
		if ('signal' in init) signal = init.signal;

		if (signal != null && !isAbortSignal(signal)) {
			throw new TypeError('Expected signal to be an instanceof AbortSignal');
		}

		this[INTERNALS$2] = {
			method,
			redirect: init.redirect || input.redirect || 'follow',
			headers,
			parsedURL,
			signal
		};

		// node-fetch-only options
		this.follow = init.follow !== undefined ? init.follow : input.follow !== undefined ? input.follow : 20;
		this.compress = init.compress !== undefined ? init.compress : input.compress !== undefined ? input.compress : true;
		this.counter = init.counter || input.counter || 0;
		this.agent = init.agent || input.agent;
	}

	get method() {
		return this[INTERNALS$2].method;
	}

	get url() {
		return format_url(this[INTERNALS$2].parsedURL);
	}

	get headers() {
		return this[INTERNALS$2].headers;
	}

	get redirect() {
		return this[INTERNALS$2].redirect;
	}

	get signal() {
		return this[INTERNALS$2].signal;
	}

	/**
  * Clone this request
  *
  * @return  Request
  */
	clone() {
		return new Request(this);
	}
}

Body.mixIn(Request.prototype);

Object.defineProperty(Request.prototype, Symbol.toStringTag, {
	value: 'Request',
	writable: false,
	enumerable: false,
	configurable: true
});

Object.defineProperties(Request.prototype, {
	method: { enumerable: true },
	url: { enumerable: true },
	headers: { enumerable: true },
	redirect: { enumerable: true },
	clone: { enumerable: true },
	signal: { enumerable: true }
});

/**
 * Convert a Request to Node.js http request options.
 *
 * @param   Request  A Request instance
 * @return  Object   The options object to be passed to http.request
 */
function getNodeRequestOptions(request) {
	const parsedURL = request[INTERNALS$2].parsedURL;
	const headers = new Headers(request[INTERNALS$2].headers);

	// fetch step 1.3
	if (!headers.has('Accept')) {
		headers.set('Accept', '*/*');
	}

	// Basic fetch
	if (!parsedURL.protocol || !parsedURL.hostname) {
		throw new TypeError('Only absolute URLs are supported');
	}

	if (!/^https?:$/.test(parsedURL.protocol)) {
		throw new TypeError('Only HTTP(S) protocols are supported');
	}

	if (request.signal && request.body instanceof Stream.Readable && !streamDestructionSupported) {
		throw new Error('Cancellation of streamed requests with AbortSignal is not supported in node < 8');
	}

	// HTTP-network-or-cache fetch steps 2.4-2.7
	let contentLengthValue = null;
	if (request.body == null && /^(POST|PUT)$/i.test(request.method)) {
		contentLengthValue = '0';
	}
	if (request.body != null) {
		const totalBytes = getTotalBytes(request);
		if (typeof totalBytes === 'number') {
			contentLengthValue = String(totalBytes);
		}
	}
	if (contentLengthValue) {
		headers.set('Content-Length', contentLengthValue);
	}

	// HTTP-network-or-cache fetch step 2.11
	if (!headers.has('User-Agent')) {
		headers.set('User-Agent', 'node-fetch/1.0 (+https://github.com/bitinn/node-fetch)');
	}

	// HTTP-network-or-cache fetch step 2.15
	if (request.compress && !headers.has('Accept-Encoding')) {
		headers.set('Accept-Encoding', 'gzip,deflate');
	}

	if (!headers.has('Connection') && !request.agent) {
		headers.set('Connection', 'close');
	}

	// HTTP-network fetch step 4.2
	// chunked encoding is handled by Node.js

	return Object.assign({}, parsedURL, {
		method: request.method,
		headers: exportNodeCompatibleHeaders(headers),
		agent: request.agent
	});
}

/**
 * abort-error.js
 *
 * AbortError interface for cancelled requests
 */

/**
 * Create AbortError instance
 *
 * @param   String      message      Error message for human
 * @return  AbortError
 */
function AbortError(message) {
  Error.call(this, message);

  this.type = 'aborted';
  this.message = message;

  // hide custom error implementation details from end-users
  Error.captureStackTrace(this, this.constructor);
}

AbortError.prototype = Object.create(Error.prototype);
AbortError.prototype.constructor = AbortError;
AbortError.prototype.name = 'AbortError';

// fix an issue where "PassThrough", "resolve" aren't a named export for node <10
const PassThrough$1 = Stream.PassThrough;
const resolve_url = Url.resolve;

/**
 * Fetch function
 *
 * @param   Mixed    url   Absolute url or Request instance
 * @param   Object   opts  Fetch options
 * @return  Promise
 */
function fetch(url, opts) {

	// allow custom promise
	if (!fetch.Promise) {
		throw new Error('native promise missing, set fetch.Promise to your favorite alternative');
	}

	Body.Promise = fetch.Promise;

	// wrap http.request into fetch
	return new fetch.Promise(function (resolve, reject) {
		// build request object
		const request = new Request(url, opts);
		const options = getNodeRequestOptions(request);

		const send = (options.protocol === 'https:' ? https : http).request;
		const signal = request.signal;

		let response = null;

		const abort = function abort() {
			let error = new AbortError('The user aborted a request.');
			reject(error);
			if (request.body && request.body instanceof Stream.Readable) {
				request.body.destroy(error);
			}
			if (!response || !response.body) return;
			response.body.emit('error', error);
		};

		if (signal && signal.aborted) {
			abort();
			return;
		}

		const abortAndFinalize = function abortAndFinalize() {
			abort();
			finalize();
		};

		// send request
		const req = send(options);
		let reqTimeout;

		if (signal) {
			signal.addEventListener('abort', abortAndFinalize);
		}

		function finalize() {
			req.abort();
			if (signal) signal.removeEventListener('abort', abortAndFinalize);
			clearTimeout(reqTimeout);
		}

		if (request.timeout) {
			req.once('socket', function (socket) {
				reqTimeout = setTimeout(function () {
					reject(new FetchError(`network timeout at: ${request.url}`, 'request-timeout'));
					finalize();
				}, request.timeout);
			});
		}

		req.on('error', function (err) {
			reject(new FetchError(`request to ${request.url} failed, reason: ${err.message}`, 'system', err));
			finalize();
		});

		req.on('response', function (res) {
			clearTimeout(reqTimeout);

			const headers = createHeadersLenient(res.headers);

			// HTTP fetch step 5
			if (fetch.isRedirect(res.statusCode)) {
				// HTTP fetch step 5.2
				const location = headers.get('Location');

				// HTTP fetch step 5.3
				const locationURL = location === null ? null : resolve_url(request.url, location);

				// HTTP fetch step 5.5
				switch (request.redirect) {
					case 'error':
						reject(new FetchError(`redirect mode is set to error: ${request.url}`, 'no-redirect'));
						finalize();
						return;
					case 'manual':
						// node-fetch-specific step: make manual redirect a bit easier to use by setting the Location header value to the resolved URL.
						if (locationURL !== null) {
							// handle corrupted header
							try {
								headers.set('Location', locationURL);
							} catch (err) {
								// istanbul ignore next: nodejs server prevent invalid response headers, we can't test this through normal request
								reject(err);
							}
						}
						break;
					case 'follow':
						// HTTP-redirect fetch step 2
						if (locationURL === null) {
							break;
						}

						// HTTP-redirect fetch step 5
						if (request.counter >= request.follow) {
							reject(new FetchError(`maximum redirect reached at: ${request.url}`, 'max-redirect'));
							finalize();
							return;
						}

						// HTTP-redirect fetch step 6 (counter increment)
						// Create a new Request object.
						const requestOpts = {
							headers: new Headers(request.headers),
							follow: request.follow,
							counter: request.counter + 1,
							agent: request.agent,
							compress: request.compress,
							method: request.method,
							body: request.body,
							signal: request.signal
						};

						// HTTP-redirect fetch step 9
						if (res.statusCode !== 303 && request.body && getTotalBytes(request) === null) {
							reject(new FetchError('Cannot follow redirect with body being a readable stream', 'unsupported-redirect'));
							finalize();
							return;
						}

						// HTTP-redirect fetch step 11
						if (res.statusCode === 303 || (res.statusCode === 301 || res.statusCode === 302) && request.method === 'POST') {
							requestOpts.method = 'GET';
							requestOpts.body = undefined;
							requestOpts.headers.delete('content-length');
						}

						// HTTP-redirect fetch step 15
						resolve(fetch(new Request(locationURL, requestOpts)));
						finalize();
						return;
				}
			}

			// prepare response
			res.once('end', function () {
				if (signal) signal.removeEventListener('abort', abortAndFinalize);
			});
			let body = res.pipe(new PassThrough$1());

			const response_options = {
				url: request.url,
				status: res.statusCode,
				statusText: res.statusMessage,
				headers: headers,
				size: request.size,
				timeout: request.timeout
			};

			// HTTP-network fetch step 12.1.1.3
			const codings = headers.get('Content-Encoding');

			// HTTP-network fetch step 12.1.1.4: handle content codings

			// in following scenarios we ignore compression support
			// 1. compression support is disabled
			// 2. HEAD request
			// 3. no Content-Encoding header
			// 4. no content response (204)
			// 5. content not modified response (304)
			if (!request.compress || request.method === 'HEAD' || codings === null || res.statusCode === 204 || res.statusCode === 304) {
				response = new Response(body, response_options);
				resolve(response);
				return;
			}

			// For Node v6+
			// Be less strict when decoding compressed responses, since sometimes
			// servers send slightly invalid responses that are still accepted
			// by common browsers.
			// Always using Z_SYNC_FLUSH is what cURL does.
			const zlibOptions = {
				flush: zlib.Z_SYNC_FLUSH,
				finishFlush: zlib.Z_SYNC_FLUSH
			};

			// for gzip
			if (codings == 'gzip' || codings == 'x-gzip') {
				body = body.pipe(zlib.createGunzip(zlibOptions));
				response = new Response(body, response_options);
				resolve(response);
				return;
			}

			// for deflate
			if (codings == 'deflate' || codings == 'x-deflate') {
				// handle the infamous raw deflate response from old servers
				// a hack for old IIS and Apache servers
				const raw = res.pipe(new PassThrough$1());
				raw.once('data', function (chunk) {
					// see http://stackoverflow.com/questions/37519828
					if ((chunk[0] & 0x0F) === 0x08) {
						body = body.pipe(zlib.createInflate());
					} else {
						body = body.pipe(zlib.createInflateRaw());
					}
					response = new Response(body, response_options);
					resolve(response);
				});
				return;
			}

			// otherwise, use response as-is
			response = new Response(body, response_options);
			resolve(response);
		});

		writeToStream(req, request);
	});
}
/**
 * Redirect code matching
 *
 * @param   Number   code  Status code
 * @return  Boolean
 */
fetch.isRedirect = function (code) {
	return code === 301 || code === 302 || code === 303 || code === 307 || code === 308;
};

// expose Promise
fetch.Promise = global.Promise;

module.exports = exports = fetch;
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = exports;
exports.Headers = Headers;
exports.Request = Request;
exports.Response = Response;
exports.FetchError = FetchError;


/***/ }),

/***/ 828:
/***/ (function(module, exports, __webpack_require__) {

/**
 * Module dependencies.
 */

const tty = __webpack_require__(885);
const util = __webpack_require__(64);

/**
 * This is the Node.js implementation of `debug()`.
 */

exports.init = init;
exports.log = log;
exports.formatArgs = formatArgs;
exports.save = save;
exports.load = load;
exports.useColors = useColors;

/**
 * Colors.
 */

exports.colors = [6, 2, 3, 4, 5, 1];

try {
	// Optional dependency (as in, doesn't need to be installed, NOT like optionalDependencies in package.json)
	// eslint-disable-next-line import/no-extraneous-dependencies
	const supportsColor = __webpack_require__(101);

	if (supportsColor && (supportsColor.stderr || supportsColor).level >= 2) {
		exports.colors = [
			20,
			21,
			26,
			27,
			32,
			33,
			38,
			39,
			40,
			41,
			42,
			43,
			44,
			45,
			56,
			57,
			62,
			63,
			68,
			69,
			74,
			75,
			76,
			77,
			78,
			79,
			80,
			81,
			92,
			93,
			98,
			99,
			112,
			113,
			128,
			129,
			134,
			135,
			148,
			149,
			160,
			161,
			162,
			163,
			164,
			165,
			166,
			167,
			168,
			169,
			170,
			171,
			172,
			173,
			178,
			179,
			184,
			185,
			196,
			197,
			198,
			199,
			200,
			201,
			202,
			203,
			204,
			205,
			206,
			207,
			208,
			209,
			214,
			215,
			220,
			221
		];
	}
} catch (error) {
	// Swallow - we only care if `supports-color` is available; it doesn't have to be.
}

/**
 * Build up the default `inspectOpts` object from the environment variables.
 *
 *   $ DEBUG_COLORS=no DEBUG_DEPTH=10 DEBUG_SHOW_HIDDEN=enabled node script.js
 */

exports.inspectOpts = Object.keys(process.env).filter(key => {
	return /^debug_/i.test(key);
}).reduce((obj, key) => {
	// Camel-case
	const prop = key
		.substring(6)
		.toLowerCase()
		.replace(/_([a-z])/g, (_, k) => {
			return k.toUpperCase();
		});

	// Coerce string value into JS value
	let val = process.env[key];
	if (/^(yes|on|true|enabled)$/i.test(val)) {
		val = true;
	} else if (/^(no|off|false|disabled)$/i.test(val)) {
		val = false;
	} else if (val === 'null') {
		val = null;
	} else {
		val = Number(val);
	}

	obj[prop] = val;
	return obj;
}, {});

/**
 * Is stdout a TTY? Colored output is enabled when `true`.
 */

function useColors() {
	return 'colors' in exports.inspectOpts ?
		Boolean(exports.inspectOpts.colors) :
		tty.isatty(process.stderr.fd);
}

/**
 * Adds ANSI color escape codes if enabled.
 *
 * @api public
 */

function formatArgs(args) {
	const {namespace: name, useColors} = this;

	if (useColors) {
		const c = this.color;
		const colorCode = '\u001B[3' + (c < 8 ? c : '8;5;' + c);
		const prefix = `  ${colorCode};1m${name} \u001B[0m`;

		args[0] = prefix + args[0].split('\n').join('\n' + prefix);
		args.push(colorCode + 'm+' + module.exports.humanize(this.diff) + '\u001B[0m');
	} else {
		args[0] = getDate() + name + ' ' + args[0];
	}
}

function getDate() {
	if (exports.inspectOpts.hideDate) {
		return '';
	}
	return new Date().toISOString() + ' ';
}

/**
 * Invokes `util.format()` with the specified arguments and writes to stderr.
 */

function log(...args) {
	return process.stderr.write(util.format(...args) + '\n');
}

/**
 * Save `namespaces`.
 *
 * @param {String} namespaces
 * @api private
 */
function save(namespaces) {
	if (namespaces) {
		process.env.DEBUG = namespaces;
	} else {
		// If you set a process.env field to null or undefined, it gets cast to the
		// string 'null' or 'undefined'. Just delete instead.
		delete process.env.DEBUG;
	}
}

/**
 * Load `namespaces`.
 *
 * @return {String} returns the previously persisted debug modes
 * @api private
 */

function load() {
	return process.env.DEBUG;
}

/**
 * Init logic for `debug` instances.
 *
 * Create a new `inspectOpts` object in case `useColors` is set
 * differently for a particular `debug` instance.
 */

function init(debug) {
	debug.inspectOpts = {};

	const keys = Object.keys(exports.inspectOpts);
	for (let i = 0; i < keys.length; i++) {
		debug.inspectOpts[keys[i]] = exports.inspectOpts[keys[i]];
	}
}

module.exports = __webpack_require__(247)(exports);

const {formatters} = module.exports;

/**
 * Map %o to `util.inspect()`, all on a single line.
 */

formatters.o = function (v) {
	this.inspectOpts.colors = this.useColors;
	return util.inspect(v, this.inspectOpts)
		.replace(/\s*\n\s*/g, ' ');
};

/**
 * Map %O to `util.inspect()`, allowing multiple lines if needed.
 */

formatters.O = function (v) {
	this.inspectOpts.colors = this.useColors;
	return util.inspect(v, this.inspectOpts);
};


/***/ }),

/***/ 834:
/***/ (function(module) {

"use strict";

module.exports = (flag, argv) => {
	argv = argv || process.argv;
	const prefix = flag.startsWith('-') ? '' : (flag.length === 1 ? '-' : '--');
	const pos = argv.indexOf(prefix + flag);
	const terminatorPos = argv.indexOf('--');
	return pos !== -1 && (terminatorPos === -1 ? true : pos < terminatorPos);
};


/***/ }),

/***/ 863:
/***/ (function(__unusedmodule, exports) {

"use strict";

Object.defineProperty(exports, "__esModule", { value: true });
var DMMF;
(function (DMMF) {
    let ModelAction;
    (function (ModelAction) {
        ModelAction["findOne"] = "findOne";
        ModelAction["findMany"] = "findMany";
        ModelAction["create"] = "create";
        ModelAction["update"] = "update";
        ModelAction["updateMany"] = "updateMany";
        ModelAction["upsert"] = "upsert";
        ModelAction["delete"] = "delete";
        ModelAction["deleteMany"] = "deleteMany";
    })(ModelAction = DMMF.ModelAction || (DMMF.ModelAction = {}));
})(DMMF = exports.DMMF || (exports.DMMF = {}));


/***/ }),

/***/ 868:
/***/ (function(module, exports, __webpack_require__) {

"use strict";

// Copyright (C) 2011-2015 John Hewson
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
Object.defineProperty(exports, "__esModule", { value: true });
var stream = __webpack_require__(688), util = __webpack_require__(64);
// convinience API
function byline(readStream, options) {
    return module.exports.createStream(readStream, options);
}
exports.default = byline;
// basic API
module.exports.createStream = function (readStream, options) {
    if (readStream) {
        return createLineStream(readStream, options);
    }
    else {
        return new LineStream(options);
    }
};
function createLineStream(readStream, options) {
    if (!readStream) {
        throw new Error('expected readStream');
    }
    if (!readStream.readable) {
        throw new Error('readStream must be readable');
    }
    var ls = new LineStream(options);
    readStream.pipe(ls);
    return ls;
}
exports.createLineStream = createLineStream;
//
// using the new node v0.10 "streams2" API
//
module.exports.LineStream = LineStream;
function LineStream(options) {
    stream.Transform.call(this, options);
    options = options || {};
    // use objectMode to stop the output from being buffered
    // which re-concatanates the lines, just without newlines.
    this._readableState.objectMode = true;
    this._lineBuffer = [];
    this._keepEmptyLines = options.keepEmptyLines || false;
    this._lastChunkEndedWithCR = false;
    // take the source's encoding if we don't have one
    this.on('pipe', function (src) {
        if (!this.encoding) {
            // but we can't do this for old-style streams
            if (src instanceof stream.Readable) {
                this.encoding = src._readableState.encoding;
            }
        }
    });
}
util.inherits(LineStream, stream.Transform);
LineStream.prototype._transform = function (chunk, encoding, done) {
    // decode binary chunks as UTF-8
    encoding = encoding || 'utf8';
    if (Buffer.isBuffer(chunk)) {
        if (encoding == 'buffer') {
            chunk = chunk.toString(); // utf8
            encoding = 'utf8';
        }
        else {
            chunk = chunk.toString(encoding);
        }
    }
    this._chunkEncoding = encoding;
    var lines = chunk.split(/\r\n|\r|\n/g);
    // don't split CRLF which spans chunks
    if (this._lastChunkEndedWithCR && chunk[0] == '\n') {
        lines.shift();
    }
    if (this._lineBuffer.length > 0) {
        this._lineBuffer[this._lineBuffer.length - 1] += lines[0];
        lines.shift();
    }
    this._lastChunkEndedWithCR = chunk[chunk.length - 1] == '\r';
    this._lineBuffer = this._lineBuffer.concat(lines);
    this._pushBuffer(encoding, 1, done);
};
LineStream.prototype._pushBuffer = function (encoding, keep, done) {
    // always buffer the last (possibly partial) line
    while (this._lineBuffer.length > keep) {
        var line = this._lineBuffer.shift();
        // skip empty lines
        if (this._keepEmptyLines || line.length > 0) {
            if (!this.push(this._reencode(line, encoding))) {
                // when the high-water mark is reached, defer pushes until the next tick
                var self = this;
                setImmediate(function () {
                    self._pushBuffer(encoding, keep, done);
                });
                return;
            }
        }
    }
    done();
};
LineStream.prototype._flush = function (done) {
    this._pushBuffer(this._chunkEncoding, 0, done);
};
// see Readable::push
LineStream.prototype._reencode = function (line, chunkEncoding) {
    if (this.encoding && this.encoding != chunkEncoding) {
        return Buffer.from(line, chunkEncoding).toString(this.encoding);
    }
    else if (this.encoding) {
        // this should be the most common case, i.e. we're using an encoded source stream
        return line;
    }
    else {
        return Buffer.from(line, chunkEncoding);
    }
};
//# sourceMappingURL=byline.js.map

/***/ }),

/***/ 885:
/***/ (function(module) {

module.exports = require("tty");

/***/ }),

/***/ 894:
/***/ (function(module, exports, __webpack_require__) {

/**
 * Module dependencies.
 */

const tty = __webpack_require__(885);
const util = __webpack_require__(64);

/**
 * This is the Node.js implementation of `debug()`.
 */

exports.init = init;
exports.log = log;
exports.formatArgs = formatArgs;
exports.save = save;
exports.load = load;
exports.useColors = useColors;

/**
 * Colors.
 */

exports.colors = [6, 2, 3, 4, 5, 1];

try {
	// Optional dependency (as in, doesn't need to be installed, NOT like optionalDependencies in package.json)
	// eslint-disable-next-line import/no-extraneous-dependencies
	const supportsColor = __webpack_require__(99);

	if (supportsColor && (supportsColor.stderr || supportsColor).level >= 2) {
		exports.colors = [
			20,
			21,
			26,
			27,
			32,
			33,
			38,
			39,
			40,
			41,
			42,
			43,
			44,
			45,
			56,
			57,
			62,
			63,
			68,
			69,
			74,
			75,
			76,
			77,
			78,
			79,
			80,
			81,
			92,
			93,
			98,
			99,
			112,
			113,
			128,
			129,
			134,
			135,
			148,
			149,
			160,
			161,
			162,
			163,
			164,
			165,
			166,
			167,
			168,
			169,
			170,
			171,
			172,
			173,
			178,
			179,
			184,
			185,
			196,
			197,
			198,
			199,
			200,
			201,
			202,
			203,
			204,
			205,
			206,
			207,
			208,
			209,
			214,
			215,
			220,
			221
		];
	}
} catch (error) {
	// Swallow - we only care if `supports-color` is available; it doesn't have to be.
}

/**
 * Build up the default `inspectOpts` object from the environment variables.
 *
 *   $ DEBUG_COLORS=no DEBUG_DEPTH=10 DEBUG_SHOW_HIDDEN=enabled node script.js
 */

exports.inspectOpts = Object.keys(process.env).filter(key => {
	return /^debug_/i.test(key);
}).reduce((obj, key) => {
	// Camel-case
	const prop = key
		.substring(6)
		.toLowerCase()
		.replace(/_([a-z])/g, (_, k) => {
			return k.toUpperCase();
		});

	// Coerce string value into JS value
	let val = process.env[key];
	if (/^(yes|on|true|enabled)$/i.test(val)) {
		val = true;
	} else if (/^(no|off|false|disabled)$/i.test(val)) {
		val = false;
	} else if (val === 'null') {
		val = null;
	} else {
		val = Number(val);
	}

	obj[prop] = val;
	return obj;
}, {});

/**
 * Is stdout a TTY? Colored output is enabled when `true`.
 */

function useColors() {
	return 'colors' in exports.inspectOpts ?
		Boolean(exports.inspectOpts.colors) :
		tty.isatty(process.stderr.fd);
}

/**
 * Adds ANSI color escape codes if enabled.
 *
 * @api public
 */

function formatArgs(args) {
	const {namespace: name, useColors} = this;

	if (useColors) {
		const c = this.color;
		const colorCode = '\u001B[3' + (c < 8 ? c : '8;5;' + c);
		const prefix = `  ${colorCode};1m${name} \u001B[0m`;

		args[0] = prefix + args[0].split('\n').join('\n' + prefix);
		args.push(colorCode + 'm+' + module.exports.humanize(this.diff) + '\u001B[0m');
	} else {
		args[0] = getDate() + name + ' ' + args[0];
	}
}

function getDate() {
	if (exports.inspectOpts.hideDate) {
		return '';
	}
	return new Date().toISOString() + ' ';
}

/**
 * Invokes `util.format()` with the specified arguments and writes to stderr.
 */

function log(...args) {
	return process.stderr.write(util.format(...args) + '\n');
}

/**
 * Save `namespaces`.
 *
 * @param {String} namespaces
 * @api private
 */
function save(namespaces) {
	if (namespaces) {
		process.env.DEBUG = namespaces;
	} else {
		// If you set a process.env field to null or undefined, it gets cast to the
		// string 'null' or 'undefined'. Just delete instead.
		delete process.env.DEBUG;
	}
}

/**
 * Load `namespaces`.
 *
 * @return {String} returns the previously persisted debug modes
 * @api private
 */

function load() {
	return process.env.DEBUG;
}

/**
 * Init logic for `debug` instances.
 *
 * Create a new `inspectOpts` object in case `useColors` is set
 * differently for a particular `debug` instance.
 */

function init(debug) {
	debug.inspectOpts = {};

	const keys = Object.keys(exports.inspectOpts);
	for (let i = 0; i < keys.length; i++) {
		debug.inspectOpts[keys[i]] = exports.inspectOpts[keys[i]];
	}
}

module.exports = __webpack_require__(171)(exports);

const {formatters} = module.exports;

/**
 * Map %o to `util.inspect()`, all on a single line.
 */

formatters.o = function (v) {
	this.inspectOpts.colors = this.useColors;
	return util.inspect(v, this.inspectOpts)
		.replace(/\s*\n\s*/g, ' ');
};

/**
 * Map %O to `util.inspect()`, allowing multiple lines if needed.
 */

formatters.O = function (v) {
	this.inspectOpts.colors = this.useColors;
	return util.inspect(v, this.inspectOpts);
};


/***/ }),

/***/ 928:
/***/ (function(module) {

"use strict";

const TEMPLATE_REGEX = /(?:\\(u[a-f\d]{4}|x[a-f\d]{2}|.))|(?:\{(~)?(\w+(?:\([^)]*\))?(?:\.\w+(?:\([^)]*\))?)*)(?:[ \t]|(?=\r?\n)))|(\})|((?:.|[\r\n\f])+?)/gi;
const STYLE_REGEX = /(?:^|\.)(\w+)(?:\(([^)]*)\))?/g;
const STRING_REGEX = /^(['"])((?:\\.|(?!\1)[^\\])*)\1$/;
const ESCAPE_REGEX = /\\(u[a-f\d]{4}|x[a-f\d]{2}|.)|([^\\])/gi;

const ESCAPES = new Map([
	['n', '\n'],
	['r', '\r'],
	['t', '\t'],
	['b', '\b'],
	['f', '\f'],
	['v', '\v'],
	['0', '\0'],
	['\\', '\\'],
	['e', '\u001B'],
	['a', '\u0007']
]);

function unescape(c) {
	if ((c[0] === 'u' && c.length === 5) || (c[0] === 'x' && c.length === 3)) {
		return String.fromCharCode(parseInt(c.slice(1), 16));
	}

	return ESCAPES.get(c) || c;
}

function parseArguments(name, args) {
	const results = [];
	const chunks = args.trim().split(/\s*,\s*/g);
	let matches;

	for (const chunk of chunks) {
		if (!isNaN(chunk)) {
			results.push(Number(chunk));
		} else if ((matches = chunk.match(STRING_REGEX))) {
			results.push(matches[2].replace(ESCAPE_REGEX, (m, escape, chr) => escape ? unescape(escape) : chr));
		} else {
			throw new Error(`Invalid Chalk template style argument: ${chunk} (in style '${name}')`);
		}
	}

	return results;
}

function parseStyle(style) {
	STYLE_REGEX.lastIndex = 0;

	const results = [];
	let matches;

	while ((matches = STYLE_REGEX.exec(style)) !== null) {
		const name = matches[1];

		if (matches[2]) {
			const args = parseArguments(name, matches[2]);
			results.push([name].concat(args));
		} else {
			results.push([name]);
		}
	}

	return results;
}

function buildStyle(chalk, styles) {
	const enabled = {};

	for (const layer of styles) {
		for (const style of layer.styles) {
			enabled[style[0]] = layer.inverse ? null : style.slice(1);
		}
	}

	let current = chalk;
	for (const styleName of Object.keys(enabled)) {
		if (Array.isArray(enabled[styleName])) {
			if (!(styleName in current)) {
				throw new Error(`Unknown Chalk style: ${styleName}`);
			}

			if (enabled[styleName].length > 0) {
				current = current[styleName].apply(current, enabled[styleName]);
			} else {
				current = current[styleName];
			}
		}
	}

	return current;
}

module.exports = (chalk, tmp) => {
	const styles = [];
	const chunks = [];
	let chunk = [];

	// eslint-disable-next-line max-params
	tmp.replace(TEMPLATE_REGEX, (m, escapeChar, inverse, style, close, chr) => {
		if (escapeChar) {
			chunk.push(unescape(escapeChar));
		} else if (style) {
			const str = chunk.join('');
			chunk = [];
			chunks.push(styles.length === 0 ? str : buildStyle(chalk, styles)(str));
			styles.push({inverse, styles: parseStyle(style)});
		} else if (close) {
			if (styles.length === 0) {
				throw new Error('Found extraneous } in Chalk template literal');
			}

			chunks.push(buildStyle(chalk, styles)(chunk.join('')));
			chunk = [];
			styles.pop();
		} else {
			chunk.push(chr);
		}
	});

	chunks.push(chunk.join(''));

	if (styles.length > 0) {
		const errMsg = `Chalk template literal is missing ${styles.length} closing bracket${styles.length === 1 ? '' : 's'} (\`}\`)`;
		throw new Error(errMsg);
	}

	return chunks.join('');
};


/***/ }),

/***/ 936:
/***/ (function(module, __unusedexports, __webpack_require__) {

"use strict";

const escapeStringRegexp = __webpack_require__(510);
const ansiStyles = __webpack_require__(103);
const stdoutColor = __webpack_require__(269).stdout;

const template = __webpack_require__(928);

const isSimpleWindowsTerm = process.platform === 'win32' && !(process.env.TERM || '').toLowerCase().startsWith('xterm');

// `supportsColor.level` → `ansiStyles.color[name]` mapping
const levelMapping = ['ansi', 'ansi', 'ansi256', 'ansi16m'];

// `color-convert` models to exclude from the Chalk API due to conflicts and such
const skipModels = new Set(['gray']);

const styles = Object.create(null);

function applyOptions(obj, options) {
	options = options || {};

	// Detect level if not set manually
	const scLevel = stdoutColor ? stdoutColor.level : 0;
	obj.level = options.level === undefined ? scLevel : options.level;
	obj.enabled = 'enabled' in options ? options.enabled : obj.level > 0;
}

function Chalk(options) {
	// We check for this.template here since calling `chalk.constructor()`
	// by itself will have a `this` of a previously constructed chalk object
	if (!this || !(this instanceof Chalk) || this.template) {
		const chalk = {};
		applyOptions(chalk, options);

		chalk.template = function () {
			const args = [].slice.call(arguments);
			return chalkTag.apply(null, [chalk.template].concat(args));
		};

		Object.setPrototypeOf(chalk, Chalk.prototype);
		Object.setPrototypeOf(chalk.template, chalk);

		chalk.template.constructor = Chalk;

		return chalk.template;
	}

	applyOptions(this, options);
}

// Use bright blue on Windows as the normal blue color is illegible
if (isSimpleWindowsTerm) {
	ansiStyles.blue.open = '\u001B[94m';
}

for (const key of Object.keys(ansiStyles)) {
	ansiStyles[key].closeRe = new RegExp(escapeStringRegexp(ansiStyles[key].close), 'g');

	styles[key] = {
		get() {
			const codes = ansiStyles[key];
			return build.call(this, this._styles ? this._styles.concat(codes) : [codes], this._empty, key);
		}
	};
}

styles.visible = {
	get() {
		return build.call(this, this._styles || [], true, 'visible');
	}
};

ansiStyles.color.closeRe = new RegExp(escapeStringRegexp(ansiStyles.color.close), 'g');
for (const model of Object.keys(ansiStyles.color.ansi)) {
	if (skipModels.has(model)) {
		continue;
	}

	styles[model] = {
		get() {
			const level = this.level;
			return function () {
				const open = ansiStyles.color[levelMapping[level]][model].apply(null, arguments);
				const codes = {
					open,
					close: ansiStyles.color.close,
					closeRe: ansiStyles.color.closeRe
				};
				return build.call(this, this._styles ? this._styles.concat(codes) : [codes], this._empty, model);
			};
		}
	};
}

ansiStyles.bgColor.closeRe = new RegExp(escapeStringRegexp(ansiStyles.bgColor.close), 'g');
for (const model of Object.keys(ansiStyles.bgColor.ansi)) {
	if (skipModels.has(model)) {
		continue;
	}

	const bgModel = 'bg' + model[0].toUpperCase() + model.slice(1);
	styles[bgModel] = {
		get() {
			const level = this.level;
			return function () {
				const open = ansiStyles.bgColor[levelMapping[level]][model].apply(null, arguments);
				const codes = {
					open,
					close: ansiStyles.bgColor.close,
					closeRe: ansiStyles.bgColor.closeRe
				};
				return build.call(this, this._styles ? this._styles.concat(codes) : [codes], this._empty, model);
			};
		}
	};
}

const proto = Object.defineProperties(() => {}, styles);

function build(_styles, _empty, key) {
	const builder = function () {
		return applyStyle.apply(builder, arguments);
	};

	builder._styles = _styles;
	builder._empty = _empty;

	const self = this;

	Object.defineProperty(builder, 'level', {
		enumerable: true,
		get() {
			return self.level;
		},
		set(level) {
			self.level = level;
		}
	});

	Object.defineProperty(builder, 'enabled', {
		enumerable: true,
		get() {
			return self.enabled;
		},
		set(enabled) {
			self.enabled = enabled;
		}
	});

	// See below for fix regarding invisible grey/dim combination on Windows
	builder.hasGrey = this.hasGrey || key === 'gray' || key === 'grey';

	// `__proto__` is used because we must return a function, but there is
	// no way to create a function with a different prototype
	builder.__proto__ = proto; // eslint-disable-line no-proto

	return builder;
}

function applyStyle() {
	// Support varags, but simply cast to string in case there's only one arg
	const args = arguments;
	const argsLen = args.length;
	let str = String(arguments[0]);

	if (argsLen === 0) {
		return '';
	}

	if (argsLen > 1) {
		// Don't slice `arguments`, it prevents V8 optimizations
		for (let a = 1; a < argsLen; a++) {
			str += ' ' + args[a];
		}
	}

	if (!this.enabled || this.level <= 0 || !str) {
		return this._empty ? '' : str;
	}

	// Turns out that on Windows dimmed gray text becomes invisible in cmd.exe,
	// see https://github.com/chalk/chalk/issues/58
	// If we're on Windows and we're dealing with a gray color, temporarily make 'dim' a noop.
	const originalDim = ansiStyles.dim.open;
	if (isSimpleWindowsTerm && this.hasGrey) {
		ansiStyles.dim.open = '';
	}

	for (const code of this._styles.slice().reverse()) {
		// Replace any instances already present with a re-opening code
		// otherwise only the part of the string until said closing code
		// will be colored, and the rest will simply be 'plain'.
		str = code.open + str.replace(code.closeRe, code.open) + code.close;

		// Close the styling before a linebreak and reopen
		// after next line to fix a bleed issue on macOS
		// https://github.com/chalk/chalk/pull/92
		str = str.replace(/\r?\n/g, `${code.close}$&${code.open}`);
	}

	// Reset the original `dim` if we changed it to work around the Windows dimmed gray issue
	ansiStyles.dim.open = originalDim;

	return str;
}

function chalkTag(chalk, strings) {
	if (!Array.isArray(strings)) {
		// If chalk() was called by itself or with a string,
		// return the string itself as a string.
		return [].slice.call(arguments, 1).join(' ');
	}

	const args = [].slice.call(arguments, 2);
	const parts = [strings.raw[0]];

	for (let i = 1; i < strings.length; i++) {
		parts.push(String(args[i - 1]).replace(/[{}\\]/g, '\\$&'));
		parts.push(String(strings.raw[i]));
	}

	return template(chalk, parts.join(''));
}

Object.defineProperties(Chalk.prototype, styles);

module.exports = Chalk(); // eslint-disable-line new-cap
module.exports.supportsColor = stdoutColor;
module.exports.default = module.exports; // For TypeScript


/***/ }),

/***/ 953:
/***/ (function(__unusedmodule, exports) {

"use strict";

// Taken from https://gist.github.com/LukeChannings/15c92cef5a016a8b21a0
/* tslint:disable */
Object.defineProperty(exports, "__esModule", { value: true });
// ensure the keys being passed is an array of key paths
// example: 'a.b' becomes ['a', 'b'] unless it was already ['a', 'b']
const keys = (ks) => (Array.isArray(ks) ? ks : ks.split('.'));
// traverse the set of keys left to right,
// returning the current value in each iteration.
// if at any point the value for the current key does not exist,
// return the default value
exports.deepGet = (o, kp, d) => keys(kp).reduce((o, k) => (o && o[k]) || d, o);
// traverse the set of keys right to left,
// returning a new object containing both properties from the object
// we were originally passed and our new property.
//
// Example:
// If o = { a: { b: { c: 1 } } }
//
// deepSet(o, ['a', 'b', 'c'], 2) will progress thus:
// 1. c = Object.assign({}, {c: 1}, { c: 2 })
// 2. b = Object.assign({}, { b: { c: 1 } }, { b: c })
// 3. returned = Object.assign({}, { a: { b: { c: 1 } } }, { a: b })
exports.deepSet = (o, kp, v) => keys(kp).reduceRight((v, k, i, ks) => Object.assign({}, exports.deepGet(o, ks.slice(0, i)), { [k]: v }), v);


/***/ }),

/***/ 957:
/***/ (function(__unusedmodule, exports, __webpack_require__) {

"use strict";

var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const chalk_1 = __importDefault(__webpack_require__(936));
const indent_string_1 = __importDefault(__webpack_require__(484));
const js_levenshtein_1 = __importDefault(__webpack_require__(500));
exports.keyBy = (collection, iteratee) => {
    return collection.reduce((acc, curr) => {
        acc[iteratee(curr)] = curr;
        return acc;
    }, {});
};
exports.ScalarTypeTable = {
    String: true,
    Int: true,
    Float: true,
    Boolean: true,
    Long: true,
    DateTime: true,
    ID: true,
    UUID: true,
    Json: true,
};
function isScalar(str) {
    if (typeof str !== 'string') {
        return false;
    }
    return exports.ScalarTypeTable[str] || false;
}
exports.isScalar = isScalar;
exports.GraphQLScalarToJSTypeTable = {
    String: 'string',
    Int: 'number',
    Float: 'number',
    Boolean: 'boolean',
    Long: 'number',
    DateTime: ['string', 'Date'],
    ID: 'string',
    UUID: 'string',
    Json: 'object',
};
exports.JSTypeToGraphQLType = {
    string: 'String',
    boolean: 'Boolean',
    object: 'Json',
};
function stringifyGraphQLType(type) {
    if (typeof type === 'string') {
        return type;
    }
    return type.name;
}
exports.stringifyGraphQLType = stringifyGraphQLType;
function wrapWithList(str, isList) {
    if (isList) {
        return `List<${str}>`;
    }
    return str;
}
exports.wrapWithList = wrapWithList;
function getGraphQLType(value, potentialType) {
    if (value === null) {
        return 'null';
    }
    if (Array.isArray(value)) {
        const scalarTypes = value.reduce((acc, val) => {
            const type = getGraphQLType(val, potentialType);
            if (!acc.includes(type)) {
                acc.push(type);
            }
            return acc;
        }, []);
        return `List<${scalarTypes.join(' | ')}>`;
    }
    const jsType = typeof value;
    if (jsType === 'number') {
        if (Math.trunc(value) === value) {
            return 'Int';
        }
        else {
            return 'Float';
        }
    }
    if (value instanceof Date) {
        return 'DateTime';
    }
    if (jsType === 'string') {
        const date = new Date(value);
        if (potentialType &&
            typeof potentialType === 'object' &&
            potentialType.values &&
            potentialType.values.includes(value)) {
            return potentialType.name;
        }
        if (date.toString() === 'Invalid Date') {
            return 'String';
        }
        if (date.toISOString() === value) {
            return 'DateTime';
        }
    }
    return exports.JSTypeToGraphQLType[jsType];
}
exports.getGraphQLType = getGraphQLType;
function graphQLToJSType(gql) {
    return exports.GraphQLScalarToJSTypeTable[gql];
}
exports.graphQLToJSType = graphQLToJSType;
function getSuggestion(str, possibilities) {
    const bestMatch = possibilities.reduce((acc, curr) => {
        const distance = js_levenshtein_1.default(str, curr);
        if (distance < acc.distance) {
            return {
                distance,
                str: curr,
            };
        }
        return acc;
    }, {
        // heuristic to be not too strict, but allow some big mistakes (<= ~ 5)
        distance: Math.min(Math.floor(str.length) * 1.1, ...possibilities.map(p => p.length * 3)),
        str: null,
    });
    return bestMatch.str;
}
exports.getSuggestion = getSuggestion;
function stringifyInputType(input, greenKeys = false) {
    if (typeof input === 'string') {
        return input;
    }
    if (input.values) {
        return `enum ${input.name} {\n${indent_string_1.default(input.values.join(', '), 2)}\n}`;
    }
    else {
        const body = indent_string_1.default(input.args // TS doesn't discriminate based on existence of fields properly
            .map(arg => {
            const key = `${arg.name}`;
            const str = `${greenKeys ? chalk_1.default.green(key) : key}${arg.isRequired ? '' : '?'}: ${chalk_1.default.white(arg.type
                .map(argType => argIsInputType(argType) ? argType.name : wrapWithList(stringifyGraphQLType(argType), arg.isList))
                .join(' | '))}`;
            if (!arg.isRequired) {
                return chalk_1.default.dim(str);
            }
            return str;
        })
            .join('\n'), 2);
        return `${chalk_1.default.dim('type')} ${chalk_1.default.bold.dim(input.name)} ${chalk_1.default.dim('{')}\n${body}\n${chalk_1.default.dim('}')}`;
    }
}
exports.stringifyInputType = stringifyInputType;
function argIsInputType(arg) {
    if (typeof arg === 'string') {
        return false;
    }
    return true;
}
function getInputTypeName(input) {
    if (typeof input === 'string') {
        return input;
    }
    return input.name;
}
exports.getInputTypeName = getInputTypeName;
function inputTypeToJson(input, isRequired, nameOnly = false) {
    if (typeof input === 'string') {
        return input;
    }
    if (input.values) {
        return input.values.join(' | ');
    }
    // TS "Trick" :/
    const inputType = input;
    // If the parent type is required and all fields are non-scalars,
    // it's very useful to show to the user, which options they actually have
    const showDeepType = isRequired && inputType.args.every(arg => !arg.isScalar) && !inputType.isWhereType && !inputType.atLeastOne;
    if (nameOnly) {
        return getInputTypeName(input);
    }
    return inputType.args.reduce((acc, curr) => {
        if (curr.name === 'Albums_every') {
            acc[curr.name + (curr.isRequired ? '' : '?')] = getInputTypeName(curr.type[0]);
        }
        else {
            acc[curr.name + (curr.isRequired ? '' : '?')] =
                curr.isRelationFilter && !showDeepType && !curr.isRequired
                    ? getInputTypeName(curr.type[0])
                    : inputTypeToJson(curr.type[0], curr.isRequired);
        }
        return acc;
    }, {});
}
exports.inputTypeToJson = inputTypeToJson;
function destroyCircular(from, seen = []) {
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
exports.destroyCircular = destroyCircular;
function unionBy(arr1, arr2, iteratee) {
    const map = {};
    for (const element of arr1) {
        map[iteratee(element)] = element;
    }
    for (const element of arr2) {
        const key = iteratee(element);
        if (!map[key]) {
            map[key] = element;
        }
    }
    return Object.values(map);
}
exports.unionBy = unionBy;
function uniqBy(arr, iteratee) {
    const map = {};
    for (const element of arr) {
        map[iteratee(element)] = element;
    }
    return Object.values(map);
}
exports.uniqBy = uniqBy;
function capitalize(str) {
    return str[0].toUpperCase() + str.slice(1);
}
exports.capitalize = capitalize;


/***/ })

/******/ },
/******/ function(__webpack_require__) { // webpackRuntimeModules
/******/ 	"use strict";
/******/ 
/******/ 	/* webpack/runtime/node module decorator */
/******/ 	!function() {
/******/ 		__webpack_require__.nmd = function(module) {
/******/ 			module.paths = [];
/******/ 			if (!module.children) module.children = [];
/******/ 			Object.defineProperty(module, 'loaded', {
/******/ 				enumerable: true,
/******/ 				get: function() { return module.l; }
/******/ 			});
/******/ 			Object.defineProperty(module, 'id', {
/******/ 				enumerable: true,
/******/ 				get: function() { return module.i; }
/******/ 			});
/******/ 			return module;
/******/ 		};
/******/ 	}();
/******/ 	
/******/ }
);