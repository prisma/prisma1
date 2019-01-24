package com.prisma.rs.jna;

import com.sun.jna.Library;

public interface JnaRustBridge extends Library {
    int select_1();
    void get_node_by_where();
}
