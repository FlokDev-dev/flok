package wbog.flok.effects;

import wbog.flok.api.FValue;
import wbog.flok.api.FlokEffect;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * All built-in effects for Flok scripts.
 * Effects registered here are available without any addon.
 *
 * Adding a new builtin: implement the lambda and call registry.register() here.
 * No other files need to change.
 */
public final class BuiltinEffects {

    private BuiltinEffects() {}

    public static void registerAll(EffectRegistry r) {

        r.register("cancel", (player, args, ctx) -> ctx.cancelEvent());

        r.register("uncancel", (player, args, ctx) -> {
            if (ctx instanceof wbog.flok.engine.runtime.ExecutionContext ec)
                ec.uncancelEvent();
        });


        r.register("send", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            player.sendMessage(colorize(args.get(0).asString()));
        });

        r.register("broadcast", (player, args, ctx) -> {
            if (args.isEmpty()) return;
            Bukkit.broadcastMessage(colorize(args.get(0).asString()));
        });

        r.register("actionbar", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(colorize(args.get(0).asString())));
        });

        r.register("title", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            String title    = colorize(args.get(0).asString());
            String subtitle = args.size() > 1 ? colorize(args.get(1).asString()) : "";
            player.sendTitle(title, subtitle, 10, 70, 20);
        });

        r.register("heal", (player, args, ctx) -> {
            if (player == null) return;
            player.setHealth(player.getMaxHealth());
        });

        r.register("set-food", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            player.setFoodLevel(clampInt(args.get(0).asInt(), 0, 20));
        });

        r.register("set-fly", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            boolean fly = args.get(0).asBoolean();
            player.setAllowFlight(fly);
            player.setFlying(fly);
        });

        r.register("set-gamemode", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            try {
                GameMode gm = GameMode.valueOf(args.get(0).asString().toUpperCase().replace('-', '_'));
                player.setGameMode(gm);
            } catch (IllegalArgumentException ignored) {}
        });

        r.register("teleport", (player, args, ctx) -> {
            if (player == null || args.size() < 3) return;
            double x = args.get(0).asNumber();
            double y = args.get(1).asNumber();
            double z = args.get(2).asNumber();
            player.teleport(new org.bukkit.Location(player.getWorld(), x, y, z));
        });

        r.register("give", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            String matName;
            int    amount = 1;
            if (args.size() >= 2) {
                matName = args.get(args.size() >= 3 ? 1 : 0).asString();
                amount  = args.size() >= 3 ? args.get(2).asInt() : args.get(1).asInt();
            } else {
                matName = args.get(0).asString();
            }
            Material mat = Material.matchMaterial(matName.toUpperCase().replace('-', '_'));
            if (mat != null && amount > 0)
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, amount));
        });

        r.register("sound", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            String key    = args.get(0).asString().toUpperCase().replace('-', '_');
            float  volume = args.size() > 1 ? (float) args.get(1).asNumber() : 1f;
            float  pitch  = args.size() > 2 ? (float) args.get(2).asNumber() : 1f;
            try {
                Sound sound = Sound.valueOf(key);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {}
        });


        r.register("potion", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            String typeName = args.get(0).asString().toUpperCase().replace('-', '_');
            int    duration = args.size() > 1 ? args.get(1).asInt() * 20 : 200; // seconds → ticks
            int    amp      = args.size() > 2 ? args.get(2).asInt() - 1 : 0;    // 1-based → 0-based
            PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (type == null) type = PotionEffectType.getByName(typeName); // legacy fallback
            if (type != null)
                player.addPotionEffect(new PotionEffect(type, duration, amp));
        });

        r.register("scoreboard-title", (player, args, ctx) -> {
            if (player == null || args.isEmpty()) return;
            ensureScoreboard(player).getObjective("flok_hud").setDisplayName(
                    colorize(args.get(0).asString()));
        });

        r.register("scoreboard-line", (player, args, ctx) -> {
            if (player == null || args.size() < 2) return;
            int    score = args.get(0).asInt();
            String text  = colorize(args.get(1).asString());
            org.bukkit.scoreboard.Scoreboard sb = ensureScoreboard(player);
            org.bukkit.scoreboard.Objective obj = sb.getObjective("flok_hud");
            if (obj == null) return;
            for (String entry : sb.getEntries()) {
                if (obj.getScore(entry).getScore() == score) sb.resetScores(entry);
            }
            obj.getScore(text).setScore(score);
        });
    }

    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static org.bukkit.scoreboard.Scoreboard ensureScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager mgr = Bukkit.getScoreboardManager();
        org.bukkit.scoreboard.Scoreboard sb = player.getScoreboard();
        if (sb == mgr.getMainScoreboard()) {
            sb = mgr.getNewScoreboard();
            player.setScoreboard(sb);
        }
        if (sb.getObjective("flok_hud") == null) {
            sb.registerNewObjective("flok_hud", "dummy",
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Server");
            sb.getObjective("flok_hud")
                    .setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        }
        return sb;
    }
}