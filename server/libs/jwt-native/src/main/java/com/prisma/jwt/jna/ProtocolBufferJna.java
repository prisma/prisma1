package com.prisma.jwt.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public class ProtocolBufferJna extends Structure {
    public Pointer error;
    public Pointer data;
    public NativeLong data_len;

    public static class ByReference extends ProtocolBufferJna implements Structure.ByReference {}
    public static class ByValue extends ProtocolBufferJna implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("error", "data", "data_len");
    }
}
