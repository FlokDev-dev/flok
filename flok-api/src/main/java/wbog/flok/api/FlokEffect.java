package wbog.flok.api;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * A custom effect (action) that can be called from .fk scripts.
 *
 * Register via {@link FlokAPI#registerEffect(String, FlokEffect)}.
 *
 * Example:
 * <pre>
 *   api.registerEffect("give-item", (player, args, ctx) -> {
 *       Material mat = Material.matchMaterial(args.get(0).asString());
 *       if (mat != null && player != null) {
 *           player.getInventory().addItem(new ItemStack(mat));
 *       }
 *   });
 * </pre>
 *
 * In a .fk script:
 * <pre>
 *   on player-join:
 *       give-item "diamond"
 * </pre>
 */
@FunctionalInterface
public interface FlokEffect {

    /**
     * Execute this effect.
     *
     * @param player The player context, may be null for console-triggered events
     * @param args   Evaluated argument values from the script call site
     * @param ctx    Script execution context — read/write local variables, check state
     */
    void execute(Player player, List<FValue> args, FlokContext ctx);
}
