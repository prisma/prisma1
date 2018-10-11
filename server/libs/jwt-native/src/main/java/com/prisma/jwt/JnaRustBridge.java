package com.prisma.jwt;

import com.sun.jna.Library;

public interface JnaRustBridge extends Library {
    ProtocolBuffer.ByReference create_token(String secret, long expiration);
}
