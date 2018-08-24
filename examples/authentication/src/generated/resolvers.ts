import { GraphQLResolveInfo } from "graphql";

export interface ResolverFn<Root, Args, Ctx, Payload> {
  (root: Root, args: Args, ctx: Ctx, info: GraphQLResolveInfo):
    | Payload
    | Promise<Payload>;
}

export interface ITypes {
  Context: any;

  QueryRoot: any;
  MutationRoot: any;
  AuthPayloadRoot: any;
  UserRoot: any;
}

export namespace IQuery {
  export type MeResolver<T extends ITypes> = ResolverFn<
    T["QueryRoot"],
    {},
    T["Context"],
    T["UserRoot"] | null
  >;

  export interface Resolver<T extends ITypes> {
    me: MeResolver<T>;
  }
}

export namespace IMutation {
  export interface ArgsSignup {
    name: string;
    email: string;
    password: string;
  }

  export type SignupResolver<T extends ITypes> = ResolverFn<
    T["MutationRoot"],
    ArgsSignup,
    T["Context"],
    T["AuthPayloadRoot"]
  >;

  export interface ArgsLogin {
    email: string;
    password: string;
  }

  export type LoginResolver<T extends ITypes> = ResolverFn<
    T["MutationRoot"],
    ArgsLogin,
    T["Context"],
    T["AuthPayloadRoot"]
  >;

  export interface Resolver<T extends ITypes> {
    signup: SignupResolver<T>;
    login: LoginResolver<T>;
  }
}

export namespace IAuthPayload {
  export type TokenResolver<T extends ITypes> = ResolverFn<
    T["AuthPayloadRoot"],
    {},
    T["Context"],
    string
  >;

  export type UserResolver<T extends ITypes> = ResolverFn<
    T["AuthPayloadRoot"],
    {},
    T["Context"],
    T["UserRoot"]
  >;

  export interface Resolver<T extends ITypes> {
    token: TokenResolver<T>;
    user: UserResolver<T>;
  }
}

export namespace IUser {
  export type IdResolver<T extends ITypes> = ResolverFn<
    T["UserRoot"],
    {},
    T["Context"],
    string
  >;

  export type EmailResolver<T extends ITypes> = ResolverFn<
    T["UserRoot"],
    {},
    T["Context"],
    string
  >;

  export type NameResolver<T extends ITypes> = ResolverFn<
    T["UserRoot"],
    {},
    T["Context"],
    string | null
  >;

  export interface Resolver<T extends ITypes> {
    id: IdResolver<T>;
    email: EmailResolver<T>;
    name: NameResolver<T>;
  }
}

export interface IResolvers<T extends ITypes> {
  Query: IQuery.Resolver<T>;
  Mutation: IMutation.Resolver<T>;
  AuthPayload: IAuthPayload.Resolver<T>;
  User: IUser.Resolver<T>;
}
