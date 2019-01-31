package com.prisma.jwt.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtocolBufferJna.ByReference create_token(String algorithm, String secret, long expiration, String allowed_target, String allowed_action);
    ProtocolBufferJna.ByReference verify_token(String token, String[] secrets, int num_secrets, String expected_target, String expected_action);

    void jwt_initialize();
    void destroy_buffer(ProtocolBufferJna.ByReference buffer);
}
