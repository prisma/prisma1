package com.prisma.rs.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    ProtobufEnvelope.ByReference get_node_by_where(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference get_nodes(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference get_related_nodes(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference get_scalar_list_values_by_node_ids(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference execute_raw(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference count_by_model(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference count_by_table(Pointer pbi, Pointer data, int len);
    ProtobufEnvelope.ByReference execute_mutaction(Pointer pbi, Pointer data, int len);

    Pointer create_interface(String database_file);

    void destroy_interface(Pointer ptr);
    void destroy(ProtobufEnvelope.ByReference data);
}
