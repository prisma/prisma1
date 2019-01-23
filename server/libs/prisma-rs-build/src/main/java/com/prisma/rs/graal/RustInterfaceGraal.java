package com.prisma.rs.graal;

import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CLibrary;

@CLibrary("prisma")
public class RustInterfaceGraal {
    @CFunction
    static native int select_1();
}
