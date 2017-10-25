package cool.graph.util.crypto

import com.github.t3hnar.bcrypt._

object Crypto {
  def hash(password: String): String = password.bcrypt

  def verify(password: String, hash: String): Boolean = password.isBcrypted(hash)
}
