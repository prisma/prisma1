import { DMMF } from './dmmf-types';
import { Dictionary } from './utils/common';
export declare class DMMFClass implements DMMF.Document {
    datamodel: DMMF.Datamodel;
    schema: DMMF.Schema;
    mappings: DMMF.Mapping[];
    queryType: DMMF.MergedOutputType;
    mutationType: DMMF.MergedOutputType;
    outputTypes: DMMF.MergedOutputType[];
    outputTypeMap: Dictionary<DMMF.MergedOutputType>;
    inputTypes: DMMF.InputType[];
    inputTypeMap: Dictionary<DMMF.InputType>;
    enumMap: Dictionary<DMMF.Enum>;
    modelMap: Dictionary<DMMF.Model>;
    constructor({ datamodel, schema, mappings }: DMMF.Document);
    getField(fieldName: string): DMMF.SchemaField<DMMF.SchemaArg>;
    protected outputTypeToMergedOutputType: (outputType: DMMF.OutputType<DMMF.SchemaArg>) => DMMF.MergedOutputType<DMMF.SchemaArg>;
    protected resolveOutputTypes(types: DMMF.MergedOutputType[]): void;
    protected resolveInputTypes(types: DMMF.InputType[]): void;
    protected resolveFieldArgumentTypes(types: DMMF.MergedOutputType[], inputTypeMap: Dictionary<DMMF.InputType>): void;
    protected getQueryType(): DMMF.MergedOutputType;
    protected getMutationType(): DMMF.MergedOutputType;
    protected getOutputTypes(): DMMF.MergedOutputType[];
    protected getEnumMap(): Dictionary<DMMF.Enum>;
    protected getModelMap(): Dictionary<DMMF.Model>;
    protected getMergedOutputTypeMap(): Dictionary<DMMF.MergedOutputType>;
    protected getInputTypeMap(): Dictionary<DMMF.InputType>;
}
