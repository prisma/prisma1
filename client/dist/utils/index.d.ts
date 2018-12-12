import { GraphQLObjectType, GraphQLSchema, GraphQLResolveInfo, GraphQLOutputType } from 'graphql';
import { Operation } from '../types';
export declare function isScalar(t: GraphQLOutputType): boolean;
export declare function getTypeForRootFieldName(rootFieldName: string, operation: Operation, schema: GraphQLSchema): GraphQLOutputType;
export declare function printDocumentFromInfo(info: GraphQLResolveInfo): string;
export declare function getExistsTypes(queryType: GraphQLObjectType): string;
export declare function getExistsFlowTypes(queryType: GraphQLObjectType): string;
export declare function getTypesAndWhere(queryType: GraphQLObjectType): {
    type: any;
    pluralFieldName: any;
    where: string;
}[];
export declare function getWhere(field: any): string;
export declare function getDeepListType(field: any): any;
