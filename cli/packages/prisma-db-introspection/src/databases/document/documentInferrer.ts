import { IInferrer } from "../../common/inferrer"
import { ISDL } from 'prisma-datamodel'

export abstract class DocumentInferrer implements IInferrer<ISDL> {
  abstract infer(model: ISDL): Promise<ISDL> 
}