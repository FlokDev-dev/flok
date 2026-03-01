package wbog.flok.commands;

import wbog.flok.FlokPlugin;
import wbog.flok.api.FValue;
import wbog.flok.engine.CompiledScript;
import wbog.flok.util.FLogger;
import wbog.flok.util.ScriptLoadResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FlokCommand implements CommandExecutor, TabCompleter {

    private static final String
        HEAD  = "§6§l",
        OK    = "§a",
        ERR   = "§c",
        WARN  = "§e",
        DIM   = "§7",
        VAL   = "§f";

    private final FlokPlugin plugin;

    public FlokCommand(FlokPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("flok.admin")) {
            sender.sendMessage(ERR + "You don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (args.length >= 2) reloadSingle(sender, args[1]);
                else                  reloadAll(sender);
            }
            case "debug" -> {
                boolean on = !FLogger.isDebugMode();
                FLogger.setDebugMode(on);
                sender.sendMessage(WARN + "Debug mode: " + (on ? OK + "ON" : ERR + "OFF"));
            }
            case "info" -> {
                sender.sendMessage(HEAD + "Flok Engine Info");
                sender.sendMessage(DIM + "Scripts:   " + VAL + plugin.getEngine().getLoadedScriptCount());
                sender.sendMessage(DIM + "Events:    " + VAL + plugin.getEngine().getEventIndex().size());
                sender.sendMessage(DIM + "Commands:  " + VAL + plugin.getEngine().getCommandIndex().size());
                sender.sendMessage(DIM + "Debug:     " + VAL + (FLogger.isDebugMode() ? "ON" : "OFF"));
                long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                sender.sendMessage(DIM + "JVM Heap:  " + VAL + used + " MB used");
            }
            case "list" -> {
                sender.sendMessage(HEAD + "Loaded Scripts");
                for (CompiledScript s : plugin.getEngine().getScripts()) {
                    sender.sendMessage(OK + "  ✓ " + VAL + s.getName()
                        + DIM + "  [events: " + s.getEventIndex().size()
                        + "  cmds: " + s.getCommandIndex().size()
                        + "  funcs: " + s.getFunctionIndex().size() + "]");
                }
                if (plugin.getEngine().getScripts().isEmpty())
                    sender.sendMessage(DIM + "  (no scripts loaded)");
            }
            case "storage" -> handleStorage(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void reloadAll(CommandSender sender) {
        plugin.reloadConfig();
        FLogger.setDebugMode(plugin.getConfig().getBoolean("debug", false));
        sender.sendMessage(HEAD + "Reloading all scripts...");
        var results = plugin.getEngine().loadAll(scriptsFolder());
        plugin.getCommandRegistrar().registerAll();

        long ok = 0, err = 0;
        for (var r : results) {
            if (r.isOk()) { ok++; sender.sendMessage(OK + "  ✓ " + VAL + r.getFileName()); }
            else           { err++; sendError(sender, r); }
        }
        if (results.isEmpty()) sender.sendMessage(WARN + "  No .fk files found in " + scriptsFolder().getPath());
        else sender.sendMessage(ok + " loaded" + (err > 0 ? ", " + ERR + err + " failed" : OK + " — all OK") + ".");
    }

    private void reloadSingle(CommandSender sender, String rawName) {
        String name = rawName.endsWith(".fk") ? rawName : rawName + ".fk";
        sender.sendMessage(WARN + "Reloading " + name + "...");
        var r = plugin.getEngine().loadSingle(scriptsFolder(), name);
        plugin.getCommandRegistrar().registerAll();
        if (r.isOk()) sender.sendMessage(OK + "✓ " + name + " reloaded.");
        else          sendError(sender, r);
    }

    private void sendError(CommandSender sender, ScriptLoadResult r) {
        sender.sendMessage(ERR + "  ✗ " + VAL + r.getFileName());
        if (r.hasLine())
            sender.sendMessage(DIM + "     └ " + ERR + "Line " + r.getErrorLine() + ": " + WARN + r.getErrorMessage());
        else
            sender.sendMessage(DIM + "     └ " + WARN + r.getErrorMessage());
        String hint = hint(r.getErrorMessage());
        if (hint != null) sender.sendMessage(DIM + "       Hint: " + hint);
    }

    private String hint(String msg) {
        if (msg == null) return null;
        String m = msg.toLowerCase();
        if (m.contains("expected indent"))    return "Block body must be indented.";
        if (m.contains("unterminated"))       return "A string or variable is not closed.";
        if (m.contains("unexpected token"))   return "Check for typos or missing keywords.";
        if (m.contains("expected colon"))     return "Block headers need a colon ':'.";
        return null;
    }

    private void handleStorage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(DIM + "Entries: " + VAL + plugin.getStorage().keys().size());
            sender.sendMessage(DIM + "Usage: /flok storage [save|reset|list|get <key>|set <key> <val>]");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "save"  -> { plugin.getStorage().forceSave(); sender.sendMessage(OK + "Saved."); }
            case "reset" -> { plugin.getStorage().reset();     sender.sendMessage(ERR + "Storage reset!"); }
            case "list"  -> {
                sender.sendMessage(HEAD + "Persistent Storage");
                var keys = new ArrayList<>(plugin.getStorage().keys());
                if (keys.isEmpty()) { sender.sendMessage(DIM + "  (empty)"); return; }
                keys.stream().sorted().forEach(k ->
                    sender.sendMessage(DIM + "  " + k + " §8= " + VAL + plugin.getStorage().get(k).asString()));
            }
            case "get"   -> {
                if (args.length < 3) { sender.sendMessage(WARN + "Usage: /flok storage get <key>"); return; }
                sender.sendMessage(DIM + args[2] + " = " + VAL + plugin.getStorage().get(args[2]).asString());
            }
            case "set"   -> {
                if (args.length < 4) { sender.sendMessage(WARN + "Usage: /flok storage set <key> <value>"); return; }
                plugin.getStorage().set(args[2], FValue.of(args[3]));
                sender.sendMessage(OK + "Set " + args[2] + " = " + args[3]);
            }
            default -> sender.sendMessage(ERR + "Unknown subcommand.");
        }
    }

    private File scriptsFolder() {
        return new File(plugin.getDataFolder(),
            plugin.getConfig().getString("scripts.folder", "scripts"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(HEAD + "Flok §r§8— Lightweight Scripting");
        sender.sendMessage(DIM + "/flok reload " + WARN + "[file]");
        sender.sendMessage(DIM + "/flok debug");
        sender.sendMessage(DIM + "/flok info");
        sender.sendMessage(DIM + "/flok list");
        sender.sendMessage(DIM + "/flok storage " + WARN + "[save|reset|list|get|set]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("flok.admin")) return List.of();
        if (args.length == 1)
            return filter(List.of("reload", "debug", "info", "list", "storage"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            List<String> files = new ArrayList<>();
            File folder = scriptsFolder();
            if (folder.isDirectory()) {
                File[] fk = folder.listFiles(f -> f.getName().endsWith(".fk"));
                if (fk != null) for (File f : fk) files.add(f.getName());
            }
            return filter(files, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("storage"))
            return filter(List.of("save", "reset", "list", "get", "set"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("storage") && args[1].equalsIgnoreCase("get"))
            return filter(new ArrayList<>(plugin.getStorage().keys()), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        return opts.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
}
