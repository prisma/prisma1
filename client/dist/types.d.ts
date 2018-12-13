import { GraphQLResolveInfo } from 'graphql';
export declare type Operation = 'query' | 'mutation' | 'subscription';
export declare type QueryOrMutation = 'query' | 'mutation';
export interface FragmentReplacement {
    field: string;
    fragment: string;
}
export interface QueryMap {
    [rootField: string]: (args?: {
        [key: string]: any;
    }, context?: {
        [key: string]: any;
    }, info?: GraphQLResolveInfo | string) => Promise<any>;
}
export interface SubscriptionMap {
    [rootField: string]: (args?: any, context?: {
        [key: string]: any;
    }, info?: GraphQLResolveInfo | string) => AsyncIterator<any> | Promise<AsyncIterator<any>>;
}
export interface ClientWithoutSchemaOptions {
    fragmentReplacements?: FragmentReplacement[];
    before?: () => void;
}
export interface Args {
    [key: string]: any;
}
export interface Context {
    [key: string]: any;
}
export interface Filter {
    [key: string]: any;
}
export interface Exists {
    [rootField: string]: (filter: Filter) => Promise<boolean>;
}
export interface BaseClientOptions {
    endpoint: string;
    secret?: string;
    debug?: boolean;
}
export interface ClientOptions extends BaseClientOptions {
    typeDefs: string;
    models: Model[];
}
export interface Model {
    name: string;
    embedded: boolean;
}
