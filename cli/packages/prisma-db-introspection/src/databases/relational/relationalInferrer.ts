import { IInferrer } from "../../common/inferrer"
import { Table } from "./relationalConnector"

export abstract class RelationalInferrer implements IInferrer<Table[]> {
  abstract infer(model: Table[]): Promise<SDL> 
}