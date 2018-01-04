import { Graphcool } from "graphcool-binding"

export interface Context {
  db: Graphcool,
  request: any
}

export interface FileMeta {
  name: string,
  size: number,
  contentType: string,
  secret: string
}
