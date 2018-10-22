package com.prisma.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class PrismaLoggingFactory implements ILoggerFactory {

    @Override
    public Logger getLogger(String name) {
        return new PrismaLogger();
    }
}