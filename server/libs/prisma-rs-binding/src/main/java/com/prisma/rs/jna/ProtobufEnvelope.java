package com.prisma.rs.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class ProtobufEnvelope extends Structure {
    public Pointer data;
    public NativeLong len;

    public static class ByReference extends ProtobufEnvelope implements Structure.ByReference {}
    public static class ByValue extends ProtobufEnvelope implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("data", "len");
    }
}
