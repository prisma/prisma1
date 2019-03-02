package com.prisma.jwt.graal;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.Pointer;
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
        CCharPointer getError();

        @CField("error")
        void setError(CCharPointer error);

        @CField("data")
        Pointer getData();

        @CField("data")
        void setData(Pointer pointer);

        @CField("data_len")
        long getDataLen();

        @CField("data_len")
        void setDataLen(long len);
    }
}
