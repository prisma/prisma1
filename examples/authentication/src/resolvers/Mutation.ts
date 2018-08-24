import { hash, compare } from "bcrypt";
import { sign } from "jsonwebtoken";
import { APP_SECRET } from "../utils";
import { IMutation } from "../generated/resolvers";
import { Types } from "./types";

export interface MutationRoot {}

export const Mutation: IMutation.Resolver<Types> = {
  signup: async (_, { name, email, password }, ctx) => {
    const hashedPassword = await hash(password, 10);
    const user = await ctx.db.createUser({
      name,
      email,
      password: hashedPassword
    });

    return {
      token: sign({ userId: user.id }, APP_SECRET),
      user
    };
  },
  login: async (_, { email, password }, ctx) => {
    const user = await ctx.db.user({ email });

    if (!user) {
      throw new Error(`No user found for email: ${email}`);
    }

    const valid = await compare(password, user.password);
    if (!valid) {
      throw new Error("Invalid password");
    }

    return {
      token: sign({ userId: user.id }, APP_SECRET),
      user
    };
  }
};
