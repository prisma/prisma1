package com.prisma.native_jdbc.graalvm;

import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CLibrary("jdbc_native_static")
public class RustInterfaceGraal {
    @CFunction
    static native void jdbc_initialize();

    @CFunction
    static native CIntegration.RustConnection newConnection(CCharPointer url);

    @CFunction
    static native CIntegration.PointerAndError prepareStatement(CIntegration.RustConnection conn, CCharPointer query);

    @CFunction
    static native CCharPointer startTransaction(CIntegration.RustConnection connection);

    @CFunction
    static native CCharPointer commitTransaction(CIntegration.RustConnection connection);

    @CFunction
    static native CCharPointer rollbackTransaction(CIntegration.RustConnection connection);

    @CFunction
    static native CCharPointer closeConnection(CIntegration.RustConnection connection);

    @CFunction
    static native CCharPointer closeStatement(CIntegration.RustStatement stmt);

    @CFunction
    static native CCharPointer executePreparedstatement(CIntegration.RustStatement stmt, CCharPointer params);

    @CFunction
    static native CCharPointer queryPreparedstatement(CIntegration.RustStatement stmt, CCharPointer params);

    @CFunction
    static native CCharPointer sqlExecute(CIntegration.RustConnection connection, CCharPointer query, CCharPointer params);

    @CFunction
    static native CCharPointer sqlQuery(CIntegration.RustConnection connection, CCharPointer query, CCharPointer params);

    @CFunction
    static native void destroy(CIntegration.PointerAndError pointerAndError);

    @CFunction
    static native void destroy_string(CCharPointer s);
}
