package util

object TroubleCharacters {
  val value: String = "¥฿" + segment(0x1F600, 0x1F64F) + segment(0x0900, 0x0930) + segment(0x20AC, 0x20C0)

  private def segment(start: Int, end: Int) = (start to end).map(Character.toChars(_).mkString).mkString
}
