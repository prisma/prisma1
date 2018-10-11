import com.prisma.jwt.JnaRustBridge
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference

object Main extends App {
  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  val library = Native.loadLibrary("jwt_native", classOf[JnaRustBridge])

  val withExp = library.create_token("some_secret", 2000)
  val noExp   = library.create_token("some_secret", -1)

  println(withExp)
  println(noExp)

  println(withExp.len)
}
