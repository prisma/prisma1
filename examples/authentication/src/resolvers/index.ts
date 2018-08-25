import { IResolvers } from "../generated/resolvers";
import { Types } from "./types";

import { Query } from "./Query";
import { Mutation } from "./Mutation";
import { AuthPayload } from "./AuthPayload";
import { User } from "./User";

export const resolvers: IResolvers<Types> = {
  Query,
  Mutation,
  AuthPayload,
  User
};
