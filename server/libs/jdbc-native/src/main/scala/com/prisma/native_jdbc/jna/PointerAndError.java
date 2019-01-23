package com.prisma.native_jdbc.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class PointerAndError extends Structure implements Structure.ByReference {
    public String error;
    public Pointer pointer;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("error", "pointer");
    }
}
