package com.prisma.shared.schema_dsl

trait TestIds {
  val testClientId           = "test-client-id"
  val testAuth0Id            = "auth0|580f939ba1bc2cc066caa46b"
  val testProjectId          = "test-project-id@test-stage"
  val testEmail              = "test-email"
  val testPassword           = "test-password"
  val testResetPasswordToken = "test-reset-password-token"
  val requestId              = "test-request-id"
  val requestIp              = "test-request-ip"
}
object TestIds extends TestIds
