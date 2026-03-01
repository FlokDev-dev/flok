package wbog.flok.api;

import org.bukkit.entity.Player;
import java.util.List;

@FunctionalInterface
public interface FlokEffect {

    void execute(Player player, List<FValue> args, FlokContext ctx);
}
