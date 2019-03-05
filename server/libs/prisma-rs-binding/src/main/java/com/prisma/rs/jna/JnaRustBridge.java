package com.prisma.rs.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtobufEnvelope.ByReference get_node_by_where(Pointer data, int len);
    ProtobufEnvelope.ByReference get_nodes(Pointer data, int len);
    ProtobufEnvelope.ByReference get_related_nodes(Pointer data, int len);

    void destroy(ProtobufEnvelope.ByReference data);
}
