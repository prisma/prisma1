package com.prisma.jwt;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtocolBuffer.ByReference create_token(String secret, long expiration);
    ProtocolBuffer.ByReference verify_token(String token, Pointer secrets, int num_secrets);

    void destroy_buffer(ProtocolBuffer.ByReference buffer);
}
