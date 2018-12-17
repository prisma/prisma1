package com.prisma.jwt.graal;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.function.CLibrary;

/*
 * This class sole purpose is to instruct the underlying compiler to include a -lm flag on Linux (Amazon Lambda).
 * Required for the underlying Rust JWT lib to compile successfully.
 */
@CLibrary("-lm")
@Platforms(Platform.LINUX.class)
public class MathPhantomLib {}