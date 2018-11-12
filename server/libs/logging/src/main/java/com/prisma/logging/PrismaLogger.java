package com.prisma.logging;

import org.slf4j.Marker;
import org.slf4j.event.Level;

// Super simple logging implementation
public class PrismaLogger implements org.slf4j.Logger {
    @Override
    public String getName() {
        return "prisma";
    }

    private static Level logLevel = getLogLevel();

    private static Level getLogLevel() {
        String env = System.getenv("LOG_LEVEL");
        if (env == null) {
            System.out.println("No log level set, defaulting to INFO.");
            return Level.INFO;

        } else {
            env = env.toUpperCase();
        }

        try {
            return Level.valueOf(env);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid log level found: " + env + ", defaulting to INFO");
            return Level.INFO;
        }
    }

    /*
     * Capabilities:
     * Implemented:     warn, error, info, debug, trace
     * Not implemented: methods with markers
     */
    @Override public boolean isInfoEnabled(Marker marker)  { return false; }
    @Override public boolean isDebugEnabled(Marker marker) { return false; }
    @Override public boolean isWarnEnabled(Marker marker)  { return false; }
    @Override public boolean isErrorEnabled(Marker marker) { return false; }
    @Override public boolean isTraceEnabled(Marker marker) { return false; }
    @Override public boolean isInfoEnabled()  { return Level.INFO.toInt() >= logLevel.toInt(); }
    @Override public boolean isDebugEnabled() { return Level.DEBUG.toInt() >= logLevel.toInt(); }
    @Override public boolean isWarnEnabled()  { return Level.WARN.toInt() >= logLevel.toInt(); }
    @Override public boolean isErrorEnabled() { return Level.ERROR.toInt() >= logLevel.toInt(); }
    @Override public boolean isTraceEnabled() { return Level.TRACE.toInt() >= logLevel.toInt(); }

    private void log(String prefix, String msg) {
        System.out.println("[" + prefix + "] " + msg);
    }

    private void log(String prefix, String format, Object arg) {
        System.out.println("[" + prefix + "] " + String.format(format, arg));
    }

    private void log(String prefix, String format, Object arg1, Object arg2) {
        System.out.println("[" + prefix + "] " + String.format(format, arg1, arg2));
    }

    private void log(String prefix, String format, Object... arguments) {
        System.out.println("[" + prefix + "] " + String.format(format, arguments));
    }

    private void log(String prefix, String msg, Throwable t) {
        System.out.println("[" + prefix + "] " + msg + "\n" + t.getMessage());
        t.printStackTrace();
    }

    @Override public void info(String msg) { log("Info", msg); }
    @Override public void info(String format, Object arg) { log("Info", format, arg); }
    @Override public void info(String format, Object arg1, Object arg2) { log("Info", format, arg1, arg2); }
    @Override public void info(String format, Object... arguments) { log("Info", format, arguments); }
    @Override public void info(String msg, Throwable t) { log("Info", msg, t); }

    @Override public void debug(String msg) { log("Debug", msg); }
    @Override public void debug(String format, Object arg) { log("Debug", format, arg); }
    @Override public void debug(String format, Object arg1, Object arg2) { log("Debug", format, arg1, arg2); }
    @Override public void debug(String format, Object... arguments) { log("Debug", format, arguments); }
    @Override public void debug(String msg, Throwable t) { log("Debug", msg, t); }

    @Override public void warn(String msg) { log("Warning", msg); }
    @Override public void warn(String format, Object arg) { log("Warning", format, arg); }
    @Override public void warn(String format, Object arg1, Object arg2) { log("Warning", format, arg1, arg2); }
    @Override public void warn(String format, Object... arguments) { log("Warning", format, arguments); }
    @Override public void warn(String msg, Throwable t) { log("Warning", msg, t); }

    @Override public void error(String msg) { log("Error", msg); }
    @Override public void error(String format, Object arg) { log("Error", format, arg); }
    @Override public void error(String format, Object arg1, Object arg2) { log("Error", format, arg1, arg2); }
    @Override public void error(String format, Object... arguments) { log("Error", format, arguments); }
    @Override public void error(String msg, Throwable t) { log("Error", msg, t); }

    @Override public void trace(String msg) { log("Trace", msg); }
    @Override public void trace(String format, Object arg) { log("Trace", format, arg); }
    @Override public void trace(String format, Object arg1, Object arg2) { log("Trace", format, arg1, arg2); }
    @Override public void trace(String format, Object... arguments) { log("Trace", format, arguments); }
    @Override public void trace(String msg, Throwable t) { log("Trace", msg, t); }

    // Todo Marker methods
    @Override public void debug(Marker marker, String msg) {}
    @Override public void debug(Marker marker, String format, Object arg) {}
    @Override public void debug(Marker marker, String format, Object arg1, Object arg2) {}
    @Override public void debug(Marker marker, String format, Object... arguments) {}
    @Override public void debug(Marker marker, String msg, Throwable t) {}

    @Override public void trace(Marker marker, String msg) {}
    @Override public void trace(Marker marker, String format, Object arg) {}
    @Override public void trace(Marker marker, String format, Object arg1, Object arg2) {}
    @Override public void trace(Marker marker, String format, Object... argArray) {}
    @Override public void trace(Marker marker, String msg, Throwable t) {}

    @Override public void warn(Marker marker, String msg) {}
    @Override public void warn(Marker marker, String format, Object arg) {}
    @Override public void warn(Marker marker, String format, Object arg1, Object arg2) {}
    @Override public void warn(Marker marker, String format, Object... arguments) {}
    @Override public void warn(Marker marker, String msg, Throwable t) {}

    @Override public void error(Marker marker, String msg) { }
    @Override public void error(Marker marker, String format, Object arg) { }
    @Override public void error(Marker marker, String format, Object arg1, Object arg2) { }
    @Override public void error(Marker marker, String format, Object... arguments) { }
    @Override public void error(Marker marker, String msg, Throwable t) { }

    @Override public void info(Marker marker, String msg) {}
    @Override public void info(Marker marker, String format, Object arg) {}
    @Override public void info(Marker marker, String format, Object arg1, Object arg2) {}
    @Override public void info(Marker marker, String format, Object... arguments) {}
    @Override public void info(Marker marker, String msg, Throwable t) {}
}