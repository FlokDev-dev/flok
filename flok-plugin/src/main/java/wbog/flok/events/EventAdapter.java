package wbog.flok.events;

import wbog.flok.api.FValue;
import wbog.flok.engine.ScriptEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.Map;

/**
 * Bridges Bukkit events into Flok's script engine.
 *
 * THROTTLING POLICY:
 * PlayerMoveEvent is registered but only forwarded to the engine if the player
 * has moved more than 0.5 blocks — purely positional events (head rotations)
 * are discarded before they ever reach script dispatch. The engine then applies
 * its own 1-second per-player throttle on top.
 *
 * All other events are player-triggered and fire at a natural, human rate.
 */
public final class EventAdapter implements Listener {

    private final ScriptEngine engine;
    private final org.bukkit.plugin.Plugin plugin;

    public EventAdapter(ScriptEngine engine, org.bukkit.plugin.Plugin plugin) {
        this.engine = engine;
        this.plugin = plugin;
    }

    // ── Player lifecycle ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        engine.dispatchEvent("player-join", p, Map.of(
                "first-join", FValue.of(!p.hasPlayedBefore())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        engine.dispatchEvent("player-quit", e.getPlayer(), Map.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        engine.dispatchEvent("player-death", e.getEntity(), Map.of(
                "death-message", FValue.of(e.getDeathMessage() != null ? e.getDeathMessage() : "")
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        engine.dispatchEvent("player-respawn", e.getPlayer(), Map.of());
    }

    // ── Player actions ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        final org.bukkit.entity.Player player = e.getPlayer();
        final String message = e.getMessage();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                engine.dispatchEvent("player-chat", player, Map.of(
                        "message", FValue.of(message)
                ), e)
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        engine.dispatchEvent("player-command", e.getPlayer(), Map.of(
                "command", FValue.of(e.getMessage())
        ), e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelChange(PlayerLevelChangeEvent e) {
        engine.dispatchEvent("player-level-change", e.getPlayer(), Map.of(
                "old-level", FValue.of(e.getOldLevel()),
                "new-level", FValue.of(e.getNewLevel())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        engine.dispatchEvent("player-gamemode-change", e.getPlayer(), Map.of(
                "new-gamemode", FValue.of(e.getNewGameMode().name().toLowerCase())
        ));
    }

    // ── Movement — double-gated for performance ────────────────────────────────
    // Gate 1 (here): ignore pure rotation events (no position change).
    // Gate 2 (ScriptEngine.dispatchEvent): 1-second per-player throttle.

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Discard head-rotation-only events before they even reach the engine
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }
        engine.dispatchEvent("player-move", e.getPlayer(), Map.of(
                "from-x", FValue.of(e.getFrom().getX()),
                "from-y", FValue.of(e.getFrom().getY()),
                "from-z", FValue.of(e.getFrom().getZ()),
                "to-x",   FValue.of(e.getTo().getX()),
                "to-y",   FValue.of(e.getTo().getY()),
                "to-z",   FValue.of(e.getTo().getZ())
        ), e);
    }

    // ── Blocks ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        engine.dispatchEvent("block-break", e.getPlayer(), Map.of(
                "block-type",  FValue.of(e.getBlock().getType().name().toLowerCase()),
                "block-x",     FValue.of(e.getBlock().getX()),
                "block-y",     FValue.of(e.getBlock().getY()),
                "block-z",     FValue.of(e.getBlock().getZ()),
                "block-world", FValue.of(e.getBlock().getWorld().getName())
        ), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        engine.dispatchEvent("block-place", e.getPlayer(), Map.of(
                "block-type",  FValue.of(e.getBlock().getType().name().toLowerCase()),
                "block-x",     FValue.of(e.getBlock().getX()),
                "block-y",     FValue.of(e.getBlock().getY()),
                "block-z",     FValue.of(e.getBlock().getZ()),
                "block-world", FValue.of(e.getBlock().getWorld().getName())
        ), e);
    }
}