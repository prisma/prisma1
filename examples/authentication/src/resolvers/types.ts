import { ITypes } from "../generated/resolvers";

import { QueryRoot } from "./Query";
import { MutationRoot } from "./Mutation";
import { AuthPayloadRoot } from "./AuthPayload";
import { UserRoot } from "./User";

import { Context } from "./Context";

export interface Types extends ITypes {
  Context: Context;
  QueryRoot: QueryRoot;
  MutationRoot: MutationRoot;
  AuthPayloadRoot: AuthPayloadRoot;
  UserRoot: UserRoot;
}
