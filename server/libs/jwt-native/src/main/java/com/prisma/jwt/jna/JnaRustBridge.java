package com.prisma.jwt.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtocolBufferJna.ByReference create_token(String algorithm, String secret, long expiration);
    ProtocolBufferJna.ByReference verify_token(String token, Pointer secrets, int num_secrets, JnaJwtGrant.ByReference grant);

    void destroy_buffer(ProtocolBufferJna.ByReference buffer);
}
