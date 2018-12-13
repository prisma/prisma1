import { Generator } from './Generator';
import { GraphQLUnionType, GraphQLInterfaceType, GraphQLInputObjectType, GraphQLScalarType, GraphQLEnumType, GraphQLObjectType as GraphQLObjectTypeRef, GraphQLField, GraphQLArgument } from 'graphql';
export declare type FieldLikeType = {
    name: string;
    typeName: string;
    type: GraphQLInputObjectType;
    typeFields: string[];
    isScalar: boolean;
    isEnum: boolean;
    args: GraphQLArgument[];
    isList: boolean;
    isNonNull: boolean;
    isInput: boolean;
};
export interface RenderOptions {
    endpoint: string;
    secret?: string;
}
export declare class GoGenerator extends Generator {
    printedTypes: {
        [key: string]: boolean;
    };
    scalarMapping: {
        Int: string;
        String: string;
        ID: string;
        Float: string;
        Boolean: string;
        DateTime: string;
        Json: string;
        Long: string;
    };
    goTypeName(fieldType: FieldLikeType): string;
    shouldOmitEmpty(fieldType: FieldLikeType): boolean;
    goStructTag(field: GraphQLField<any, any>): string;
    extractFieldLikeType(field: GraphQLField<any, any>): FieldLikeType;
    graphqlRenderers: {
        GraphQLUnionType: (type: GraphQLUnionType) => string;
        GraphQLObjectType: (type: GraphQLObjectTypeRef | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLInterfaceType: (type: GraphQLObjectTypeRef | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLInputObjectType: (type: GraphQLObjectTypeRef | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLScalarType: (type: GraphQLScalarType) => string;
        GraphQLIDType: (type: GraphQLScalarType) => string;
        GraphQLEnumType: (type: GraphQLEnumType) => string;
    };
    getDeepType(type: any): any;
    graphqlTypeRenderersForQuery: {
        GraphQLScalarType: (type: GraphQLScalarType) => string;
        GraphQLObjectType: (type: GraphQLObjectTypeRef) => string;
        GraphQLInterfaceType: (type: GraphQLInterfaceType) => string;
        GraphQLUnionType: (type: GraphQLUnionType) => string;
        GraphQLEnumType: (type: GraphQLEnumType) => string;
        GraphQLInputObjectType: (type: GraphQLInputObjectType) => string;
    };
    opUpdateMany(field: any): string;
    opUpdate(field: any): string;
    opDeleteMany(field: any): string;
    opDelete(field: any): string;
    opGetOne(field: any): string;
    opGetMany(field: any): string;
    opGetConnection(field: any): string;
    opCreate(field: any): string;
    opUpsert(field: any): string;
    paramsType(field: any, verb?: string): {
        code: string;
        type: string;
    };
    printOperation(fields: any, operation: string, options: RenderOptions): string;
    printEndpoint(options: RenderOptions): string;
    printSecret(options: RenderOptions): string | null;
    render(options: RenderOptions): string;
}
