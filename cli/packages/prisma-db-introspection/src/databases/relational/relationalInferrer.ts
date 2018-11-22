import { IInferrer } from "../../common/inferrer"
import { Table } from "./relationalConnector"
import { ISDL } from 'prisma-datamodel'

export abstract class RelationalInferrer implements IInferrer<Table[]> {
  abstract infer(model: Table[]): Promise<ISDL> 
}