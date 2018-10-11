package com.prisma.jwt;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public class ProtocolBuffer extends Structure {
    public boolean success;
    public NativeLong len;
    public Pointer data;

    public static class ByReference extends ProtocolBuffer implements Structure.ByReference {}
    public static class ByValue extends ProtocolBuffer implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("success", "len", "data");
    }
}
