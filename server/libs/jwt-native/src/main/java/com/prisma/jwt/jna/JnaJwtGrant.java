package com.prisma.jwt.jna;

import com.prisma.jwt.JwtGrant;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;


public class JnaJwtGrant extends Structure {
    public String target;
    public String action;

    public void setFrom(JwtGrant grant) {
        this.target = grant.target();
        this.action = grant.action();
    }

    public static class ByReference extends JnaJwtGrant implements Structure.ByReference {}

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("target", "action");
    }
}
