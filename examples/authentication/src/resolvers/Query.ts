import { IQuery } from "../generated/resolvers";
import { getUserId } from "../utils";
import { Types } from "./types";

export interface QueryRoot {}

export const Query: IQuery.Resolver<Types> = {
  me: (_, {}, ctx) => {
    return ctx.db.user({ id: getUserId(ctx) });
  }
};
