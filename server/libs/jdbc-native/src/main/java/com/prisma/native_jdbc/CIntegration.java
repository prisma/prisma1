//package com.prisma;
//
//import org.graalvm.nativeimage.c.CContext;
//import org.graalvm.nativeimage.c.struct.CField;
//import org.graalvm.nativeimage.c.struct.CStruct;
//import org.graalvm.word.PointerBase;
//
//import java.util.Collections;
//import java.util.List;
//
//
//@CContext(CIntegration.CIntegrationDirectives.class)
//public class CIntegration {
//
//    static class CIntegrationDirectives implements CContext.Directives {
//
//        @Override
//        public List<String> getHeaderFiles() {
//            /*
//             * The header file with the C declarations that are imported. We use a helper class that
//             * locates the file in our project structure.
//             */
//            return Collections.singletonList("\"" + System.getProperty("user.dir") + "/hello.h\"");
//        }
//    }
//
//
//    @CStruct("Counter")
//    public interface Counter extends PointerBase {
//        @CField("count")
//        long getCount();
//    }
//
//    @CStruct(value = "PsqlConnection", isIncomplete = true)
//    public interface RustConnection extends PointerBase {}
//}
