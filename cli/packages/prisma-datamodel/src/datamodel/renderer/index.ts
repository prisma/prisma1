import Renderer from "./renderer"
import { DatabaseType } from '../../databaseType'

export default abstract class Renderers {
  public static create(databaseType: DatabaseType) : Renderer {
    switch(databaseType) {
      default: return new Renderer()
    }
  }
}