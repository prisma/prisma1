import { IInferrer } from "../../common/inferrer"

export abstract class DocumentInferrer implements IInferrer<any> {
  abstract infer(model: Table[]): Promise<SDL> 
}