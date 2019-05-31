import Arg from 'arg'

export abstract class Command<T extends Arg.Spec> {
  abstract run(flags: Arg.Result<T>): Promise<string | Error>
  abstract flags: T
}
