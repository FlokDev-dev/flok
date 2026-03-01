package wbog.flok.commands;

import wbog.flok.engine.ScriptEngine;
import wbog.flok.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DynamicCommandRegistrar {

    private final JavaPlugin plugin;
    private final ScriptEngine engine;
    private final Map<String, ScriptCommand> registered = new HashMap<>();

    public DynamicCommandRegistrar(JavaPlugin plugin, ScriptEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    public void registerAll() {
        CommandMap map = getCommandMap();
        if (map == null) {
            FLogger.error("Could not access Bukkit CommandMap script commands will not work!");
            return;
        }

        for (ScriptCommand cmd : registered.values()) cmd.unregister(map);
        registered.clear();

        for (var entry : engine.getCommandIndex().entrySet()) {
            String name = entry.getKey();
            var block = entry.getValue().block();
            ScriptCommand cmd = new ScriptCommand(name, engine);
            if (block.description() != null && !block.description().isEmpty())
                cmd.setDescription(block.description());
            if (!block.aliases().isEmpty())
                cmd.setAliases(block.aliases());
            map.register(plugin.getName().toLowerCase(), cmd);
            registered.put(name, cmd);
            FLogger.debug("Registered command: /" + name);
        }

        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (Exception ignored) {}

        FLogger.info("Registered " + registered.size() + " script command(s).");
    }

    private CommandMap getCommandMap() {
        try { return Bukkit.getServer().getCommandMap(); } catch (Exception ignored) {}
        try {
            var field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            FLogger.error("CommandMap reflection failed: " + e.getMessage());
            return null;
        }
    }

    private static final class ScriptCommand extends Command {

        private final ScriptEngine engine;

        ScriptCommand(String name, ScriptEngine engine) {
            super(name);
            this.engine = engine;
            setDescription("Flok script command: /" + name);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            var entry = engine.getCommandIndex().get(getName());
            if (entry != null) {
                String perm = entry.block().permission();
                if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
                    player.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
            }
            try {
                engine.dispatchCommand(getName(), player, args);
            } catch (Exception e) {
                FLogger.error("Error in command /" + getName() + ": " + e.getMessage());
                sender.sendMessage("§cAn internal error occurred.");
            }
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return new ArrayList<>();
        }
    }
}