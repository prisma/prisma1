package com.prisma.jwt.graal;

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
            return Collections.singletonList("<" + CIntegration.class.getClassLoader().getResource("jwt_native.h").getPath() + ">");
        }
    }

    @CStruct(value = "ProtocolBuffer")
    public interface ProtocolBuffer extends PointerBase {
        @CField("error")
        CCharPointer error();

        @CField("data")
        CCharPointer data();

        @CField("data_len")
        CCharPointer data_len();
    }

    @CStruct(value = "ExtGrant")
    public interface ExtGrant extends PointerBase {
        @CField("target")
        CCharPointer target();

        @CField("action")
        CCharPointer action();
    }
}
