export type TypeIdentifier =
  | 'String'
  | 'Int'
  | 'Float'
  | 'Boolean'
  | 'Long' // Long is sometimes used internally by prisma.
  | 'DateTime'
  | 'ID'
  | 'UUID'
  | 'Json' // | 'Enum' | 'Relation'

export abstract class TypeIdentifiers {
  public static string: TypeIdentifier = 'String'
  public static integer: TypeIdentifier = 'Int'
  public static float: TypeIdentifier = 'Float'
  public static boolean: TypeIdentifier = 'Boolean'
  public static long: TypeIdentifier = 'Long'
  public static dateTime: TypeIdentifier = 'DateTime'
  public static id: TypeIdentifier = 'ID'
  public static uuid: TypeIdentifier = 'UUID'
  public static json: TypeIdentifier = 'Json'
}
