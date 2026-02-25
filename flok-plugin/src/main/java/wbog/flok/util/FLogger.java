package wbog.flok.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Centralized logger for Flok.
 */
public final class FLogger {

    private static Logger  logger    = null;
    private static boolean debugMode = false;

    private FLogger() {}

    public static void init(JavaPlugin plugin) {
        logger    = plugin.getLogger();
        debugMode = plugin.getConfig().getBoolean("debug", false);
    }

    public static void info(String msg)  { if (logger != null) logger.info(msg); }
    public static void warn(String msg)  { if (logger != null) logger.warning(msg); }
    public static void error(String msg) { if (logger != null) logger.severe(msg); }

    public static void debug(String msg) {
        if (debugMode && logger != null) logger.info("[DEBUG] " + msg);
    }

    public static void scriptError(String script, int line, String message) {
        if (logger != null)
            logger.severe("[Script Error] " + script + " (line " + line + "): " + message);
    }

    public static void setDebugMode(boolean debug) { debugMode = debug; }
    public static boolean isDebugMode()            { return debugMode; }
}
