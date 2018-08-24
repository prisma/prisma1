import { IAuthPayload } from "../generated/resolvers";
import { Types } from "./types";
import { UserRoot } from "./User";

export interface AuthPayloadRoot {
  token: string;
  user: UserRoot;
}

export const AuthPayload: IAuthPayload.Resolver<Types> = {
  token: root => root.token,
  user: root => root.user
};
