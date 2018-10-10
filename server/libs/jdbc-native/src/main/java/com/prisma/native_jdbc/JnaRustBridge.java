package com.prisma.native_jdbc;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface JnaRustBridge extends Library {
    PointerAndError prepareStatement(Pointer connection, String query);

    Pointer newConnection(String url);

    String startTransaction(Pointer connection);

    String commitTransaction(Pointer connection);

    String rollbackTransaction(Pointer connection);

    String closeConnection(Pointer connection);

    String sqlExecute(Pointer connection, String query, String params);

    String sqlQuery(Pointer connection, String query, String params);

    String executePreparedstatement(Pointer stmt, String params);

    String queryPreparedstatement(Pointer stmt, String params);
}

