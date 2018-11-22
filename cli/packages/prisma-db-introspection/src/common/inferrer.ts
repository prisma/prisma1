
export interface IInferrer<InputType> {
  infer(model: InputType): Promise<SDL>
}