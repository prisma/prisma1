package com.prisma.native_jdbc.graalvm;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

import java.util.Collections;
import java.util.List;


@CContext(CIntegration.CIntegrationDirectives.class)
public class CIntegration {

    static class CIntegrationDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList("<" + CIntegration.class.getClassLoader().getResource("jdbc_native.h").getPath() + ">");
        }
    }

    @CStruct(value = "PsqlConnection", isIncomplete = true)
    public interface RustConnection extends PointerBase {}

    @CStruct(value = "PsqlPreparedStatement", isIncomplete = true)
    public interface RustStatement extends PointerBase {}

    @CStruct(value = "PointerAndError")
    public interface PointerAndError extends PointerBase {
        @CField("error")
        CCharPointer error();

        @CField("pointer")
        PointerBase pointer();
    }
}
