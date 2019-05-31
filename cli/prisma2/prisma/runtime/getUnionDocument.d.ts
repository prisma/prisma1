import { DMMF } from './dmmf-types';
/**
 * Turns type: string into type: string[] for all args in order to support union input types
 * @param document
 */
export declare function getUnionDocument(document: DMMF.Document<DMMF.RawSchemaArg>): DMMF.Document<DMMF.SchemaArg>;
