package com.prisma.jwt.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtocolBufferJna.ByReference create_token(String algorithm, String secret, long expiration, JnaJwtGrant.ByReference grant);
    ProtocolBufferJna.ByReference verify_token(String token, Pointer secrets, int num_secrets, JnaJwtGrant.ByReference grant);

    void initialize();
    void destroy_buffer(ProtocolBufferJna.ByReference buffer);
}
