package com.prisma.jwt

import org.joda.time.DateTime

object Main extends App {
  val binding = NativeBinding.jna()

  println("---------------------- Creation ----------------------")
  val secrets         = Vector("some_secret", "some_other_secret", "another_one")
  val tokenWithExp    = binding.createToken("some_secret", Some(DateTime.now().plusHours(1).getMillis / 1000)).get
  val tokenWithoutExp = binding.createToken("some_secret", None).get

  println(tokenWithExp)
  println(tokenWithoutExp)

  println("---------------------- Validation ----------------------")

  val verify1Result = binding.verifyToken(tokenWithExp, secrets)
  val verify2Result = binding.verifyToken(tokenWithoutExp, secrets)
  val verify3Result = binding.verifyToken("someinvalidtoken", secrets)
  val verify4Result = binding.verifyToken(tokenWithExp, Vector("invalidsecret"))

  println(verify1Result)
  println(verify2Result)
  println(verify3Result)
  println(verify4Result)

//  assert(verify1Result.success)
//  assert(verify2Result.success)
//  assert(!verify3Result.success)
//  assert(!verify4Result.success)
//
//  library.destroy_buffer(verify1Result)
//  library.destroy_buffer(verify2Result)
//  library.destroy_buffer(verify3Result)
//  library.destroy_buffer(verify4Result)
}
