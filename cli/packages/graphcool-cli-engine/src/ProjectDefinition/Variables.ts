'use strict'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Output } from '../Output/index'

import * as _ from 'lodash'
import * as replaceall from 'replaceall'
import * as BbPromise from 'bluebird'
import { Args } from '../types/common'

export default class Variables {
  json: any
  overwriteSyntax: RegExp = RegExp(/,/g)
  envRefSyntax: RegExp = RegExp(/^env:/g)
  selfRefSyntax: RegExp = RegExp(/^self:/g)
  stringRefSyntax: RegExp = RegExp(/('.*')|(".*")/g)
  optRefSyntax: RegExp = RegExp(/^opt:/g)
  variableSyntax: RegExp = RegExp(
    /* tslint:disable-next-line */
    '\\${([ ~:a-zA-Z0-9._\'",\\-\\/\\(\\)]+?)}',
    'g',
  )
  out: Output
  fileName: string
  options: Args

  constructor(
    out: Output,
    fileName: string,
    options: Args = {}
  ) {
    this.out = out
    this.fileName = fileName
    this.options = options

    // this.fileRefSyntax = RegExp(/^file\((~?[a-zA-Z0-9._\-/]+?)\)/g);
    // this.cfRefSyntax = RegExp(/^cf:/g);
    // this.s3RefSyntax = RegExp(/^s3:(.+?)\/(.+)$/);
    // this.ssmRefSyntax = RegExp(/^ssm:([a-zA-Z0-9_.-/]+)[~]?(true|false)?/);
  }

  populateJson(json: any): Promise<any> {
    this.json = json
    return this.populateObject(this.json).then(() => {
      return BbPromise.resolve(this.json)
    })
  }

  public populateObject(objectToPopulate) {
    const populateAll: any[] = []
    const deepMapValues = (object, callback, propertyPath?: string[]) => {
      const deepMapValuesIteratee = (value, key) =>
        deepMapValues(
          value,
          callback,
          propertyPath ? propertyPath.concat(key) : [key],
        )
      if (_.isArray(object)) {
        return _.map(object, deepMapValuesIteratee)
      } else if (
        _.isObject(object) &&
        !_.isDate(object) &&
        !_.isRegExp(object) &&
        !_.isFunction(object)
      ) {
        return _.extend({}, object, _.mapValues(object, deepMapValuesIteratee))
      }
      return callback(object, propertyPath)
    }

    deepMapValues(objectToPopulate, (property, propertyPath) => {
      if (typeof property === 'string') {
        const populateSingleProperty = this.populateProperty(property, true)
          .then(newProperty =>
            _.set(objectToPopulate, propertyPath, newProperty),
          )
          .return()
        populateAll.push(populateSingleProperty)
      }
    })

    return BbPromise.all(populateAll).then(() => objectToPopulate)
  }

  populateProperty(propertyParam, populateInPlace?: boolean) {
    let property = populateInPlace ? propertyParam : _.cloneDeep(propertyParam)
    const allValuesToPopulate: any[] = []

    if (typeof property === 'string' && property.match(this.variableSyntax)) {
      property.match(this.variableSyntax)!.forEach(matchedString => {
        const variableString = matchedString
          .replace(this.variableSyntax, (match, varName) => varName.trim())
          .replace(/\s/g, '')

        let singleValueToPopulate: Promise<any> | null = null
        if (variableString.match(this.overwriteSyntax)) {
          singleValueToPopulate = this.overwrite(variableString)
        } else {
          singleValueToPopulate = this.getValueFromSource(
            variableString,
          ).then(valueToPopulate => {
            if (typeof valueToPopulate === 'object') {
              return this.populateObject(valueToPopulate)
            }
            return valueToPopulate
          })
        }

        singleValueToPopulate = singleValueToPopulate!.then(valueToPopulate => {
          this.warnIfNotFound(variableString, valueToPopulate)
          return this.populateVariable(
            property,
            matchedString,
            valueToPopulate,
          ).then(newProperty => {
            property = newProperty
            return BbPromise.resolve(property)
          })
        })

        allValuesToPopulate.push(singleValueToPopulate)
      })
      return BbPromise.all(allValuesToPopulate).then(() => {
        if ((property as any) !== (this.json as any)) {
          return this.populateProperty(property)
        }
        return BbPromise.resolve(property)
      })
    }
    // return property;
    return BbPromise.resolve(property)
  }

  populateVariable(propertyParam, matchedString, valueToPopulate) {
    let property = propertyParam
    if (typeof valueToPopulate === 'string') {
      property = replaceall(matchedString, valueToPopulate, property)
    } else {
      if (property !== matchedString) {
        if (typeof valueToPopulate === 'number') {
          property = replaceall(
            matchedString,
            String(valueToPopulate),
            property,
          )
        } else {
          const errorMessage = [
            'Trying to populate non string value into',
            ` a string for variable ${matchedString}.`,
            ' Please make sure the value of the property is a string.',
          ].join('')
          this.out.warn(
            this.out.getErrorPrefix(this.fileName, 'warning') + errorMessage,
          )
        }
        return BbPromise.resolve(property)
      }
      property = valueToPopulate
    }
    return BbPromise.resolve(property)
  }

  overwrite(variableStringsString) {
    let finalValue
    const variableStringsArray = variableStringsString.split(',')
    const allValuesFromSource = variableStringsArray.map(variableString =>
      this.getValueFromSource(variableString),
    )
    return BbPromise.all(allValuesFromSource).then(valuesFromSources => {
      valuesFromSources.find(valueFromSource => {
        finalValue = valueFromSource
        return (
          finalValue !== null &&
          typeof finalValue !== 'undefined' &&
          !(typeof finalValue === 'object' && _.isEmpty(finalValue))
        )
      })
      return BbPromise.resolve(finalValue)
    })
  }

  getValueFromSource(variableString) {
    if (variableString.match(this.envRefSyntax)) {
      return this.getValueFromEnv(variableString)
    } else if (variableString.match(this.optRefSyntax)) {
      return this.getValueFromOptions(variableString)
    } else if (variableString.match(this.selfRefSyntax)) {
      return this.getValueFromSelf(variableString)
      // } else if (variableString.match(this.fileRefSyntax)) {
      //   return this.getValueFromFile(variableString);
      // } else if (variableString.match(this.cfRefSyntax)) {
      //   return this.getValueFromCf(variableString);
      // } else if (variableString.match(this.s3RefSyntax)) {
      //   return this.getValueFromS3(variableString);
    } else if (variableString.match(this.stringRefSyntax)) {
      return this.getValueFromString(variableString)
      // } else if (variableString.match(this.ssmRefSyntax)) {
      //   return this.getValueFromSsm(variableString);
    }
    const errorMessage = [
      `Invalid variable reference syntax for variable ${variableString}.`,
      ' You can only reference env vars, options, & files.',
      ' You can check our docs for more info.',
    ].join('')
    this.out.warn(
      this.out.getErrorPrefix(this.fileName, 'warning') + errorMessage,
    )
  }

  getValueFromEnv(variableString) {
    const requestedEnvVar = variableString.split(':')[1]
    const valueToPopulate =
      requestedEnvVar !== '' || '' in process.env
        ? process.env[requestedEnvVar]
        : process.env
    return BbPromise.resolve(valueToPopulate)
  }

  getValueFromString(variableString) {
    const valueToPopulate = variableString.replace(/^['"]|['"]$/g, '')
    return BbPromise.resolve(valueToPopulate)
  }

  getValueFromOptions(variableString) {
    const requestedOption = variableString.split(':')[1];
    const valueToPopulate = (requestedOption !== '' || '' in this.options) ? this.options[requestedOption] : this.options
    return BbPromise.resolve(valueToPopulate);
  }

  getValueFromSelf(variableString) {
    const valueToPopulate = this.json
    const deepProperties = variableString.split(':')[1].split('.')
    return this.getDeepValue(deepProperties, valueToPopulate)
  }

  // getValueFromFile(variableString) {
  //   const matchedFileRefString = variableString.match(this.fileRefSyntax)[0];
  //   const referencedFileRelativePath = matchedFileRefString
  //     .replace(this.fileRefSyntax, (match, varName) => varName.trim())
  //     .replace('~', os.homedir());
  //
  //   const referencedFileFullPath = (path.isAbsolute(referencedFileRelativePath) ?
  //     referencedFileRelativePath :
  //     path.join(this.graphcool.config.servicePath, referencedFileRelativePath));
  //   let fileExtension = referencedFileRelativePath.split('.');
  //   fileExtension = fileExtension[fileExtension.length - 1];
  //   // Validate file exists
  //   if (!this.graphcool.utils.fileExistsSync(referencedFileFullPath)) {
  //     return BbPromise.resolve(undefined);
  //   }
  //
  //   let valueToPopulate;
  //
  //   // Process JS files
  //   if (fileExtension === 'js') {
  //     const jsFile = require(referencedFileFullPath); // eslint-disable-line global-require
  //     const variableArray = variableString.split(':');
  //     let returnValueFunction;
  //     if (variableArray[1]) {
  //       let jsModule = variableArray[1];
  //       jsModule = jsModule.split('.')[0];
  //       returnValueFunction = jsFile[jsModule];
  //     } else {
  //       returnValueFunction = jsFile;
  //     }
  //
  //     if (typeof returnValueFunction !== 'function') {
  //       throw new this.graphcool.classes
  //         .Error([
  //           'Invalid variable syntax when referencing',
  //           ` file "${referencedFileRelativePath}".`,
  //           ' Check if your javascript is exporting a function that returns a value.',
  //         ].join(''));
  //     }
  //     valueToPopulate = returnValueFunction.call(jsFile);
  //
  //     return BbPromise.resolve(valueToPopulate).then(valueToPopulateResolved => {
  //       let deepProperties = variableString.replace(matchedFileRefString, '');
  //       deepProperties = deepProperties.slice(1).split('.');
  //       deepProperties.splice(0, 1);
  //       return this.getDeepValue(deepProperties, valueToPopulateResolved)
  //         .then(deepValueToPopulateResolved => {
  //           if (typeof deepValueToPopulateResolved === 'undefined') {
  //             const errorMessage = [
  //               'Invalid variable syntax when referencing',
  //               ` file "${referencedFileRelativePath}".`,
  //               ' Check if your javascript is returning the correct data.',
  //             ].join('');
  //             throw new this.graphcool.classes
  //               .Error(errorMessage);
  //           }
  //           return BbPromise.resolve(deepValueToPopulateResolved);
  //         });
  //     });
  //   }
  //
  //   // Process everything except JS
  //   if (fileExtension !== 'js') {
  //     valueToPopulate = this.graphcool.utils.readFileSync(referencedFileFullPath);
  //     if (matchedFileRefString !== variableString) {
  //       let deepProperties = variableString
  //         .replace(matchedFileRefString, '');
  //       if (deepProperties.substring(0, 1) !== ':') {
  //         const errorMessage = [
  //           'Invalid variable syntax when referencing',
  //           ` file "${referencedFileRelativePath}" sub properties`,
  //           ' Please use ":" to reference sub properties.',
  //         ].join('');
  //         throw new this.graphcool.classes
  //           .Error(errorMessage);
  //       }
  //       deepProperties = deepProperties.slice(1).split('.');
  //       return this.getDeepValue(deepProperties, valueToPopulate);
  //     }
  //   }
  //   return BbPromise.resolve(valueToPopulate);
  // }
  //
  // getValueFromCf(variableString) {
  //   const variableStringWithoutSource = variableString.split(':')[1].split('.');
  //   const stackName = variableStringWithoutSource[0];
  //   const outputLogicalId = variableStringWithoutSource[1];
  //   return this.graphcool.getProvider('aws')
  //     .request('CloudFormation',
  //       'describeStacks',
  //       { StackName: stackName },
  //       this.options.stage,
  //       this.options.region)
  //     .then(result => {
  //       const outputs = result.Stacks[0].Outputs;
  //       const output = outputs.find(x => x.OutputKey === outputLogicalId);
  //
  //       if (output === undefined) {
  //         const errorMessage = [
  //           'Trying to request a non exported variable from CloudFormation.',
  //           ` Stack name: "${stackName}"`,
  //           ` Requested variable: "${outputLogicalId}".`,
  //         ].join('');
  //         throw new this.graphcool.classes
  //           .Error(errorMessage);
  //       }
  //
  //       return output.OutputValue;
  //     });
  // }
  //
  // getValueFromS3(variableString) {
  //   const groups = variableString.match(this.s3RefSyntax);
  //   const bucket = groups[1];
  //   const key = groups[2];
  //   return this.graphcool.getProvider('aws')
  //     .request('S3',
  //       'getObject',
  //       {
  //         Bucket: bucket,
  //         Key: key,
  //       },
  //       this.options.stage,
  //       this.options.region)
  //     .then(
  //       response => response.Body.toString(),
  //       err => {
  //         const errorMessage = `Error getting value for ${variableString}. ${err.message}`;
  //         throw new this.graphcool.classes.Error(errorMessage);
  //       }
  //     );
  // }
  //
  // getValueFromSsm(variableString) {
  //   const groups = variableString.match(this.ssmRefSyntax);
  //   const param = groups[1];
  //   const decrypt = (groups[2] === 'true');
  //   return this.graphcool.getProvider('aws')
  //     .request('SSM',
  //       'getParameter',
  //       {
  //         Name: param,
  //         WithDecryption: decrypt,
  //       },
  //       this.options.stage,
  //       this.options.region)
  //     .then(
  //       response => BbPromise.resolve(response.Parameter.Value),
  //       err => {
  //         const expectedErrorMessage = `Parameter ${param} not found.`;
  //         if (err.message !== expectedErrorMessage) {
  //           throw new this.graphcool.classes.Error(err.message);
  //         }
  //         return BbPromise.resolve(undefined);
  //       }
  //     );
  // }

  getDeepValue(deepProperties, valueToPopulate) {
    return BbPromise.reduce(
      deepProperties,
      (computedValueToPopulateParam, subProperty) => {
        let computedValueToPopulate = computedValueToPopulateParam
        if (typeof computedValueToPopulate === 'undefined') {
          computedValueToPopulate = {}
        } else if (subProperty !== '' || '' in computedValueToPopulate) {
          computedValueToPopulate = computedValueToPopulate[subProperty]
        }
        if (
          typeof computedValueToPopulate === 'string' &&
          computedValueToPopulate.match(this.variableSyntax)
        ) {
          return this.populateProperty(computedValueToPopulate)
        }
        return BbPromise.resolve(computedValueToPopulate)
      },
      valueToPopulate,
    )
  }

  warnIfNotFound(variableString, valueToPopulate) {
    if (
      valueToPopulate === null ||
      typeof valueToPopulate === 'undefined' ||
      (typeof valueToPopulate === 'object' && _.isEmpty(valueToPopulate))
    ) {
      let varType
      if (variableString.match(this.envRefSyntax)) {
        varType = 'environment variable'
      } else if (variableString.match(this.optRefSyntax)) {
        varType = 'option'
      } else if (variableString.match(this.selfRefSyntax)) {
        varType = 'self reference'
        // } else if (variableString.match(this.fileRefSyntax)) {
        //   varType = 'file';
        // } else if (variableString.match(this.ssmRefSyntax)) {
        //   varType = 'SSM parameter';
      }
      this.out.warn(
        this.out.getErrorPrefix(this.fileName, 'warning') +
          `A valid ${varType} to satisfy the declaration '${variableString}' could not be found.`,
      )
    }
  }
}
