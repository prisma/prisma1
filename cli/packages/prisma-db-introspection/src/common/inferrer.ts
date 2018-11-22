import { ISDL } from 'prisma-datamodel'

export interface IInferrer<InputType> {
  infer(model: InputType): Promise<ISDL>
}