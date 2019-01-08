package com.prisma.jwt.graal;

import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

@CLibrary("jwt_native_static")
public class GraalRustBridge {
    @CFunction
    static native CIntegration.ProtocolBuffer create_token(CCharPointer algorithm, CCharPointer secret, long expiration_in_seconds, CCharPointer allowed_target, CCharPointer allowed_action);

    @CFunction
    static native CIntegration.ProtocolBuffer verify_token(CCharPointer algorithm, CCharPointerPointer secrets, long num_secrets, CCharPointer expect_target, CCharPointer expect_action);

    @CFunction
    static native void destroy_buffer(CIntegration.ProtocolBuffer buffer);

    @CFunction
    static native void jwt_initialize();
}
