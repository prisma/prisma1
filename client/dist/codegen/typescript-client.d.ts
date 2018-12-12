import { GraphQLObjectType, GraphQLUnionType, GraphQLInterfaceType, GraphQLInputObjectType, GraphQLInputField, GraphQLField, GraphQLInputType, GraphQLOutputType, GraphQLScalarType, GraphQLEnumType, GraphQLFieldMap, GraphQLObjectType as GraphQLObjectTypeRef } from 'graphql';
import { Generator } from './Generator';
import * as prettier from 'prettier';
export interface RenderOptions {
    endpoint?: string;
    secret?: string;
}
export declare class TypescriptGenerator extends Generator {
    genericsDelimiter: string;
    lineBreakDelimiter: string;
    partialType: string;
    prismaInterface: string;
    exportPrisma: boolean;
    scalarMapping: {
        Int: string;
        String: string;
        ID: string;
        Float: string;
        Boolean: string;
        DateTimeInput: string;
        DateTimeOutput: string;
        Json: string;
    };
    graphqlRenderers: {
        GraphQLUnionType: (type: GraphQLUnionType) => string;
        GraphQLObjectType: (type: GraphQLObjectType | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLInterfaceType: (type: GraphQLObjectType | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLInputObjectType: (type: GraphQLObjectType | GraphQLInterfaceType | GraphQLInputObjectType) => string;
        GraphQLScalarType: (type: GraphQLScalarType) => string;
        GraphQLIDType: (type: GraphQLScalarType) => string;
        GraphQLEnumType: (type: GraphQLEnumType) => string;
    };
    format(code: string, options?: prettier.Options): string;
    renderAtLeastOne(): string;
    renderModels(): string;
    render(options?: RenderOptions): string;
    renderClientConstructor(): string;
    renderImports(): string;
    renderPrismaClassArgs(options?: RenderOptions): string;
    renderExports(options?: RenderOptions): string;
    renderTypedefs(): string;
    renderExists(): string;
    renderQueries(): string;
    renderMutations(): string;
    renderSubscriptions(): string;
    getTypeNames(): string[];
    renderTypes(): any;
    renderArgs(field: GraphQLField<any, any>, isMutation?: boolean, isTopLevel?: boolean): any;
    renderInputFieldTypeHelper(field: any, isMutation: any): any;
    renderMainMethodFields(operation: string, fields: GraphQLFieldMap<any, any>, isMutation?: boolean): string;
    getDeepType(type: any): any;
    getInternalTypeName(type: any): string;
    getPayloadType(operation: string): "Promise<AsyncIterator<T>>" | "Promise<T>";
    isEmbeddedType(type: GraphQLScalarType | GraphQLObjectType | GraphQLEnumType): boolean;
    renderInterfaceOrObject(type: GraphQLObjectTypeRef | GraphQLInputObjectType | GraphQLInterfaceType, node?: boolean, subscription?: boolean): string;
    renderFieldName(field: GraphQLInputField | GraphQLField<any, any>, node: boolean): string;
    wrapType(type: any, subscription?: boolean, isArray?: boolean): string;
    renderFieldType({ field, node, input, partial, renderFunction, isMutation, isSubscription, operation, embedded, }: {
        field: any;
        node: boolean;
        input: boolean;
        partial: boolean;
        renderFunction: boolean;
        isMutation: boolean;
        isSubscription?: boolean;
        operation: boolean;
        embedded: boolean;
    }): any;
    renderInputFieldType(type: GraphQLInputType | GraphQLOutputType): any;
    renderInputListType(type: any): string;
    renderTypeWrapper(typeName: string, typeDescription: string | void, fieldDefinition: string): string;
    renderInterfaceWrapper(typeName: string, typeDescription: string | void, interfaces: GraphQLInterfaceType[], fieldDefinition: string, promise?: boolean, subscription?: boolean): string;
    renderDescription(description?: string | void): string;
    isConnectionType(type: GraphQLObjectTypeRef | GraphQLInputObjectType | GraphQLInterfaceType): boolean;
    renderConnectionType(type: GraphQLObjectTypeRef): string;
    isSubscriptionType(type: GraphQLObjectType | GraphQLInputObjectType | GraphQLInterfaceType): boolean;
    private renderSubscriptionType;
}
