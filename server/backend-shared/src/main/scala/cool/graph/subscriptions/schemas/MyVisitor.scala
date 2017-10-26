package cool.graph.subscriptions.schemas

import sangria.ast._

/**
  * Limitations of the Ast Transformer
  * - Only the onEnter callback can change nodes
  * - the onLeave callback gets called with the old children
  * - no skip or break functionality anymore
  * - comments can't be transformed
  *
  * All these limitations could be eliminated. However, that would take much more effort and would make the code
  * much more complex.
  */
object MyAstVisitor {

  def visitAst(
      doc: AstNode,
      onEnter: AstNode ⇒ Option[AstNode] = _ ⇒ None,
      onLeave: AstNode ⇒ Option[AstNode] = _ ⇒ None
  ): AstNode = {

    def breakOrSkip(cmd: Option[AstNode]) = cmd match {
      case _ =>
        true
    }

    def map(cmd: Option[AstNode], originalNode: AstNode): AstNode = cmd match {
      case Some(x) =>
        x
      case None =>
        originalNode
    }

    // necessary as `Value` is a sealed trait, which can't be used in instanceOf
    def mapValues(values: Vector[AstNode]) = {
      values.map(collectValue)
    }

    def collectValue(value: AstNode) = value match {
      case x @ IntValue(_, _, _) =>
        x
      case x @ BigIntValue(_, _, _) =>
        x
      case x @ FloatValue(_, _, _) =>
        x
      case x @ BigDecimalValue(_, _, _) =>
        x
      case x @ StringValue(_, _, _) =>
        x
      case x @ BooleanValue(_, _, _) =>
        x
      case x @ EnumValue(_, _, _) =>
        x
      case x @ ListValue(_, _, _) =>
        x
      case x @ VariableValue(_, _, _) =>
        x
      case x @ NullValue(_, _) =>
        x
      case x @ ObjectValue(_, _, _) =>
        x
      // this case is only to trick the compiler and shouldn't occur
      case _ =>
        value.asInstanceOf[ObjectValue]
    }

    def loop(node: AstNode): AstNode =
      node match {
        case n @ Document(defs, trailingComments, _, _) ⇒
          var newDefs = defs
          val cmd     = onEnter(n).asInstanceOf[Option[Document]]
          cmd match {
            case None =>
              newDefs = defs.map(d ⇒ loop(d).asInstanceOf[Definition])
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newDefs = newN.definitions.map(d ⇒ loop(d).asInstanceOf[Definition])
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          if (breakOrSkip(cmd)) {
            newDefs = defs.map(d ⇒ loop(d).asInstanceOf[Definition])
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n).asInstanceOf[Document].copy(definitions = newDefs)
        case n @ FragmentDefinition(_, cond, dirs, sels, comments, trailingComments, _) ⇒
          val cmd                 = onEnter(n).asInstanceOf[Option[FragmentDefinition]]
          var newDirs             = dirs
          var newSels             = sels
          var newComments         = comments
          var newTrailingComments = trailingComments
          loop(cond)
          cmd match {
            case None =>
              newDirs = dirs.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = sels.map(s ⇒ loop(s).asInstanceOf[Selection])
              newComments = comments.map(s ⇒ loop(s).asInstanceOf[Comment])
              newTrailingComments = trailingComments.map(s ⇒ loop(s).asInstanceOf[Comment])
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newDirs = newN.directives.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = newN.selections.map(s ⇒ loop(s).asInstanceOf[Selection])
              newComments = newN.comments.map(s ⇒ loop(s).asInstanceOf[Comment])
              newTrailingComments = newN.trailingComments.map(s ⇒ loop(s).asInstanceOf[Comment])
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n)
            .asInstanceOf[FragmentDefinition]
            .copy(directives = newDirs, selections = newSels, comments = newComments, trailingComments = newTrailingComments)
        case n @ OperationDefinition(_, _, vars, dirs, sels, comment, trailingComments, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[OperationDefinition]]
          var newVars = vars
          var newDirs = dirs
          var newSels = sels

          cmd match {
            case None =>
              newVars = vars.map(d ⇒ loop(d).asInstanceOf[VariableDefinition])
              newDirs = dirs.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = sels.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newVars = newN.variables.map(d ⇒ loop(d).asInstanceOf[VariableDefinition])
              newDirs = newN.directives.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = newN.selections.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n)
            .asInstanceOf[OperationDefinition]
            .copy(variables = newVars, directives = newDirs, selections = newSels)
        case n @ VariableDefinition(_, tpe, default, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(onEnter(n))) {
            loop(tpe)
            default.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ InlineFragment(cond, dirs, sels, comment, trailingComments, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[InlineFragment]]
          var newDirs = dirs
          var newSels = sels
          cmd match {
            case None =>
              cond.foreach(c ⇒ loop(c))
              newDirs = dirs.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = sels.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newN.typeCondition.foreach(c ⇒ loop(c))
              newDirs = newN.directives.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = newN.selections.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
          }
          map(cmd, n).asInstanceOf[InlineFragment].copy(directives = newDirs, selections = newSels)
        case n @ FragmentSpread(_, dirs, comment, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[FragmentSpread]]
          var newDirs = dirs
          cmd match {
            case None =>
              newDirs = dirs.map(d ⇒ loop(d).asInstanceOf[Directive])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newDirs = newN.directives.map(d ⇒ loop(d).asInstanceOf[Directive])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n).asInstanceOf[FragmentSpread].copy(directives = newDirs)
        case n @ NotNullType(ofType, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            loop(ofType)
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ ListType(ofType, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            loop(ofType)
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ Field(_, _, args, dirs, sels, comment, trailingComments, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[Field]]
          var newArgs = args
          var newDirs = dirs
          var newSels = sels
          cmd match {
            case None =>
              newArgs = args.map(d ⇒ loop(d).asInstanceOf[Argument])
              newDirs = dirs.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = sels.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newArgs = newN.arguments.map(d ⇒ loop(d).asInstanceOf[Argument])
              newDirs = newN.directives.map(d ⇒ loop(d).asInstanceOf[Directive])
              newSels = newN.selections.map(s ⇒ loop(s).asInstanceOf[Selection])
              comment.foreach(s ⇒ loop(s))
              trailingComments.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n).asInstanceOf[Field].copy(arguments = newArgs, directives = newDirs, selections = newSels)
        case n @ Argument(_, v, comment, _) ⇒
          val cmd  = onEnter(n)
          var newV = v
          if (breakOrSkip(cmd)) {
            newV = collectValue(loop(v))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n).asInstanceOf[Argument].copy(value = newV)
        case n @ ObjectField(_, v, comment, _) ⇒
          val cmd  = onEnter(n)
          val newV = collectValue(loop(v))
          comment.foreach(s ⇒ loop(s))
          breakOrSkip(onLeave(n))
          cmd match {
            case None =>
              n.copy(value = newV)
            case Some(newN) =>
              newN
          }
        case n @ Directive(_, args, comment, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[Directive]]
          var newArgs = args
          cmd match {
            case None =>
              newArgs = args.map(d ⇒ loop(d).asInstanceOf[Argument])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newArgs = newN.arguments.map(d ⇒ loop(d).asInstanceOf[Argument])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n).asInstanceOf[Directive].copy(arguments = newArgs)
        case n @ ListValue(vals, comment, _) ⇒
          val cmd     = onEnter(n).asInstanceOf[Option[ListValue]]
          var newVals = vals
          cmd match {
            case None =>
              newVals = mapValues(vals.map(v ⇒ loop(v)))
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newVals = mapValues(newN.values.map(v ⇒ loop(v)))
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
          }
          map(cmd, n).asInstanceOf[ListValue].copy(values = newVals)
        case n @ ObjectValue(fields, comment, _) ⇒
          val cmd       = onEnter(n).asInstanceOf[Option[ObjectValue]]
          var newFields = fields
          cmd match {
            case None =>
              newFields = fields.map(f ⇒ loop(f).asInstanceOf[ObjectField])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(n))
            case Some(newN) =>
              newFields = newN.fields.map(f ⇒ loop(f).asInstanceOf[ObjectField])
              comment.foreach(s ⇒ loop(s))
              breakOrSkip(onLeave(newN))
          }
          map(cmd, n).asInstanceOf[ObjectValue].copy(fields = newFields)
        case n @ BigDecimalValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ BooleanValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ Comment(_, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            breakOrSkip(onLeave(n))
          }
          n
        case n @ VariableValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ EnumValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ NullValue(comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ NamedType(_, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ StringValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ BigIntValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ IntValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)
        case n @ FloatValue(_, comment, _) ⇒
          val cmd = onEnter(n)
          if (breakOrSkip(cmd)) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          map(cmd, n)

        // IDL schema definition

        case n @ ScalarTypeDefinition(_, dirs, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ FieldDefinition(name, fieldType, args, dirs, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            loop(fieldType)
            args.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ InputValueDefinition(_, valueType, default, dirs, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            loop(valueType)
            default.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ ObjectTypeDefinition(_, interfaces, fields, dirs, comment, trailingComments, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            interfaces.foreach(d ⇒ loop(d))
            fields.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ InterfaceTypeDefinition(_, fields, dirs, comment, trailingComments, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            fields.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ UnionTypeDefinition(_, types, dirs, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            types.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ EnumTypeDefinition(_, values, dirs, comment, trailingComments, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            values.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ EnumValueDefinition(_, dirs, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ InputObjectTypeDefinition(_, fields, dirs, comment, trailingComments, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            fields.foreach(d ⇒ loop(d))
            dirs.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ TypeExtensionDefinition(definition, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            loop(definition)
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ DirectiveDefinition(_, args, locations, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            args.foreach(d ⇒ loop(d))
            locations.foreach(d ⇒ loop(d))
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ DirectiveLocation(_, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ SchemaDefinition(ops, dirs, comment, trailingComments, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            ops.foreach(s ⇒ loop(s))
            dirs.foreach(s ⇒ loop(s))
            comment.foreach(s ⇒ loop(s))
            trailingComments.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n @ OperationTypeDefinition(_, tpe, comment, _) ⇒
          if (breakOrSkip(onEnter(n))) {
            loop(tpe)
            comment.foreach(s ⇒ loop(s))
            breakOrSkip(onLeave(n))
          }
          n
        case n => n
      }

//    breakable {
    loop(doc)
//    }

  }
}

object MyAstVisitorCommand extends Enumeration {
  val Skip, Continue, Break, Transform = Value
}
