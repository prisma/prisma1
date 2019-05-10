package com.prisma.native_jdbc.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    void jdbc_initialize();

    PointerAndError prepareStatement(Pointer connection, String query);

    Pointer newConnection(String url);

    Pointer startTransaction(Pointer connection);

    Pointer commitTransaction(Pointer connection);

    Pointer rollbackTransaction(Pointer connection);

    Pointer closeConnection(Pointer connection);

    Pointer sqlExecute(Pointer connection, String query, String params);

    Pointer sqlQuery(Pointer connection, String query, String params);

    Pointer executePreparedstatement(Pointer stmt, String params);

    Pointer queryPreparedstatement(Pointer stmt, String params);

    Pointer closeStatement(Pointer stmt);

    void destroy(PointerAndError pointerAndError);

    void destroy_string(Pointer s);
}
