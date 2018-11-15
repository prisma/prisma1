package com.prisma.jwt.graal;


import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;

@CLibrary("jdbc_native")
public class GraalRustBridge {
    @CFunction
    static native CIntegration.ProtocolBuffer create_token(CCharPointer algorithm, CCharPointer secret, CLongPointer expiration_in_seconds /* ... */);

    /// ...
}
