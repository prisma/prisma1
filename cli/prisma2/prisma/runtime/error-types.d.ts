import { DMMF } from './dmmf-types';
export interface ArgError {
    path: string[];
    error: InvalidArgError;
}
export interface FieldError {
    path: string[];
    error: InvalidFieldError;
}
export declare type InvalidFieldError = InvalidFieldNameError | InvalidFieldTypeError;
export interface InvalidFieldTypeError {
    type: 'invalidFieldType';
    modelName: string;
    fieldName: string;
    providedValue: any;
}
export interface InvalidFieldNameError {
    type: 'invalidFieldName';
    modelName: string;
    didYouMean?: string;
    providedName: string;
}
export declare type JavaScriptPrimitiveType = 'number' | 'string' | 'boolean';
export declare type InvalidArgError = InvalidArgNameError | MissingArgError | InvalidArgTypeError | AtLeastOneError | AtMostOneError;
/**
 * This error occurs if the user provides an arg name that doens't exist
 */
export interface InvalidArgNameError {
    type: 'invalidName';
    providedName: string;
    providedValue: any;
    didYouMeanArg?: string;
    didYouMeanField?: string;
    originalType: DMMF.ArgType;
    possibilities?: DMMF.ArgType[];
    outputType?: DMMF.OutputType;
}
/**
 * Opposite of InvalidArgNameError - if the user *doesn't* provide an arg that should be provided
 * This error both happens with an implicit and explicit `undefined`
 */
export interface MissingArgError {
    type: 'missingArg';
    missingName: string;
    missingType: DMMF.ArgType[];
    isScalar: boolean;
    isEnum: boolean;
    isList: boolean;
    isRequired: boolean;
    atLeastOne: boolean;
    atMostOne: boolean;
}
export interface AtMostOneError {
    type: 'atMostOne';
    key: string;
    inputType: DMMF.InputType;
    providedKeys: string[];
}
export interface AtLeastOneError {
    type: 'atLeastOne';
    key: string;
    inputType: DMMF.InputType;
}
/**
 * If the scalar type of an arg is not matching what is required
 */
export interface InvalidArgTypeError {
    type: 'invalidType';
    argName: string;
    requiredType: {
        bestFittingType: DMMF.ArgType;
        types: DMMF.ArgType[];
        isRequired: boolean;
        isScalar: boolean;
        isEnum: boolean;
        isList: boolean;
    };
    providedValue: any;
}
