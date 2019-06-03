export declare namespace DMMF {
    interface Document<T extends BaseSchemaArg = SchemaArg> {
        datamodel: Datamodel;
        schema: Schema<T>;
        mappings: Mapping[];
    }
    interface Enum {
        name: string;
        values: string[];
    }
    interface Datamodel {
        models: Model[];
        enums: Enum[];
    }
    interface Model {
        name: string;
        isEmbedded: boolean;
        dbName: string;
        fields: Field[];
    }
    type FieldKind = 'scalar' | 'relation' | 'enum';
    interface Field {
        kind: FieldKind;
        name: string;
        isRequired: boolean;
        isList: boolean;
        isUnique: boolean;
        isId: boolean;
        type: string;
    }
    interface Schema<T extends BaseSchemaArg = SchemaArg> {
        queries: Array<Query<T>>;
        mutations: Array<Query<T>>;
        inputTypes: Array<InputType<T>>;
        outputTypes: Array<OutputType<T>>;
        enums: Enum[];
    }
    interface Query<T extends BaseSchemaArg = SchemaArg> {
        name: string;
        args: T[];
        output: QueryOutput;
    }
    interface QueryOutput {
        name: string;
        isRequired: boolean;
        isList: boolean;
    }
    type ArgType<T extends BaseSchemaArg = SchemaArg> = string | InputType<T> | Enum;
    interface BaseSchemaArg {
        name: string;
        type: ArgType | ArgType[];
        isScalar: boolean;
        isRequired: boolean;
        isEnum: boolean;
        isList: boolean;
    }
    interface RawSchemaArg extends BaseSchemaArg {
        name: string;
        type: ArgType;
        isScalar: boolean;
        isRequired: boolean;
        isEnum: boolean;
        isList: boolean;
    }
    interface SchemaArg extends BaseSchemaArg {
        name: string;
        type: ArgType[];
        isScalar: boolean;
        isRequired: boolean;
        isEnum: boolean;
        isList: boolean;
        isRelationFilter?: boolean;
    }
    interface OutputType<T extends BaseSchemaArg = SchemaArg> {
        name: string;
        fields: Array<SchemaField<T>>;
    }
    interface MergedOutputType<T extends BaseSchemaArg = SchemaArg> extends OutputType<T> {
        isEmbedded: boolean;
        fields: Array<SchemaField<T>>;
    }
    interface SchemaField<T extends BaseSchemaArg = SchemaArg> {
        name: string;
        type: string | MergedOutputType<T> | Enum;
        isList: boolean;
        isRequired: boolean;
        kind: FieldKind;
        args: T[];
    }
    interface InputType<T extends BaseSchemaArg = SchemaArg> {
        name: string;
        isWhereType?: boolean;
        isOrderType?: boolean;
        atLeastOne?: boolean;
        atMostOne?: boolean;
        args: T[];
    }
    interface Mapping {
        model: string;
        findOne?: string;
        findMany?: string;
        create?: string;
        update?: string;
        updateMany?: string;
        upsert?: string;
        delete?: string;
        deleteMany?: string;
    }
    enum ModelAction {
        findOne = "findOne",
        findMany = "findMany",
        create = "create",
        update = "update",
        updateMany = "updateMany",
        upsert = "upsert",
        delete = "delete",
        deleteMany = "deleteMany"
    }
}
export interface BaseField<T extends DMMF.BaseSchemaArg = DMMF.SchemaArg> {
    name: string;
    type: string | DMMF.Enum | DMMF.MergedOutputType<T> | T['type'];
    isList: boolean;
    isRequired: boolean;
}
