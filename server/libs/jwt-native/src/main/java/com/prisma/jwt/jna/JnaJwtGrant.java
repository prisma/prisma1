package com.prisma.jwt.jna;

import com.prisma.jwt.Grant;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public class JnaJwtGrant extends Structure implements Grant {

    public String target;
    public String grant;

    public JnaJwtGrant() {}

    public JnaJwtGrant(String target, String grant) {
        this.target = target;
        this.grant = grant;
    }

    public static class ByReference extends JnaJwtGrant implements Structure.ByReference {}
    public static class ByValue extends JnaJwtGrant implements Structure.ByValue {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("target", "grant");
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getGrant() {
        return grant;
    }
}
