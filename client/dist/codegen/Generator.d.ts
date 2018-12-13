import { GraphQLSchema } from 'graphql';
import { Interpolation } from './types';
import { IGQLType } from 'prisma-datamodel';
export interface GeneratorInput {
    schema: GraphQLSchema;
    internalTypes: IGQLType[];
}
export declare class Generator {
    schema: GraphQLSchema;
    internalTypes: IGQLType[];
    constructor({ schema, internalTypes }: GeneratorInput);
    compile(strings: TemplateStringsArray, ...interpolations: Interpolation<Generator>[]): string;
}
