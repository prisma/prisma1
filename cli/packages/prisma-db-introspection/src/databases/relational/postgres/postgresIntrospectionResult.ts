import { ITable, ITableRelation } from "../relationalConnector"
import { RelationalIntrospectionResult } from "../relationalIntrospectionResult"
import { ISDL, IGQLField, IGQLType, IDirectiveInfo, plural, camelCase, capitalize, DatabaseType, Renderer, TypeIdentifier } from 'prisma-datamodel'
import * as _ from 'lodash'

export class PostgresIntrospectionResult extends RelationalIntrospectionResult {
  constructor(model: ITable[], relations: ITableRelation[], renderer?: Renderer) {
    super(model, relations, DatabaseType.postgres, renderer)
  }

  protected isTypeReserved(type: IGQLType): boolean {
    throw new Error("Method not implemented.");
  }
  protected toTypeIdentifyer(typeName: string): TypeIdentifier {
    throw new Error("Method not implemented.");
  }

}
