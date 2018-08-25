import { IUser } from "../generated/resolvers";
import { Types } from "./types";

export interface UserRoot {
  id: string;
  email: string;
  name?: string;
}

export const User: IUser.Resolver<Types> = {
  id: root => root.id,
  email: root => root.email,
  name: root => root.name
};
