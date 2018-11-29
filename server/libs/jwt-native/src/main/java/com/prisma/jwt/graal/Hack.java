package com.prisma.jwt.graal;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.function.CLibrary;

@CLibrary("-framework Security")
@Platforms(Platform.DARWIN.class)
public class Hack {

}