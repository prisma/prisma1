import { Model } from './types';
export declare function makePrismaClientClass<T>({ typeDefs, endpoint, secret, models, }: {
    typeDefs: string;
    endpoint: string;
    secret?: string;
    models: Model[];
}): T;
