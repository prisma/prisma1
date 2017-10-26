package cool.graph.util.debug

import language.experimental.macros

import reflect.macros.blackbox.Context

trait DebugMacros {
  def debug(params: Any*): Unit = macro DebugMacros.debug_impl
}

object DebugMacros extends DebugMacros {
  def hello(): Unit = macro hello_impl

  def hello_impl(c: Context)(): c.Expr[Unit] = {
    import c.universe._
    reify { println("Hello World!") }
  }

  def printparam(param: Any): Unit = macro printparam_impl

  def printparam_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    reify { println(param.splice) }
  }

  def debug1(param: Any): Unit = macro debug1_impl

  def debug1_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._
    val paramRep     = show(param.tree)
    val paramRepTree = Literal(Constant(paramRep))
    val paramRepExpr = c.Expr[String](paramRepTree)
    reify { println(paramRepExpr.splice + " = " + param.splice) }
  }

  def debug_impl(c: Context)(params: c.Expr[Any]*): c.Expr[Unit] = {
    import c.universe._

    val trees = params.map { param =>
      param.tree match {
        // Keeping constants as-is
        // The c.universe prefixes aren't necessary, but otherwise Idea keeps importing weird stuff ...
        case c.universe.Literal(c.universe.Constant(const)) => {
          val reified = reify { print(param.splice) }
          reified.tree
        }
        case _ => {
          val paramRep     = show(param.tree)
          val paramRepTree = Literal(Constant(paramRep))
          val paramRepExpr = c.Expr[String](paramRepTree)
          val reified      = reify { print(paramRepExpr.splice + " = " + param.splice) }
          reified.tree
        }
      }
    }

    // Inserting ", " between trees, and a println at the end.
    val separators          = (1 to trees.size - 1).map(_ => (reify { print(", ") }).tree) :+ (reify { println() }).tree
    val treesWithSeparators = trees.zip(separators).flatMap(p => List(p._1, p._2))

    c.Expr[Unit](Block(treesWithSeparators.toList, Literal(Constant(()))))
  }
}
