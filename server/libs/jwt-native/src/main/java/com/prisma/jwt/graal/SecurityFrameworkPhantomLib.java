package com.prisma.jwt.graal;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.function.CLibrary;

/*
 * This class sole purpose is to instruct the underlying compiler to include a -framework Security flag on OSX.
 * Required for the underlying Rust JWT lib to compile successfully.
 */
@CLibrary("-framework Security")
@Platforms(Platform.DARWIN.class)
public class SecurityFrameworkPhantomLib {}