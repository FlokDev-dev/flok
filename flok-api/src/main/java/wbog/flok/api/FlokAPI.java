package wbog.flok.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;

public interface FlokAPI {
    void registerEffect(String name, FlokEffect effect);
    void unregisterEffect(String name);
    void fireEvent(String eventName, Player player, Map<String, FValue> params);

    default void fireEvent(String eventName, Player player) {
        fireEvent(eventName, player, Map.of());
    }

    boolean hasEventHandler(String eventName);
    boolean hasCommand(String commandName);

    int getLoadedScriptCount();

    FValue getStorage(String key);

    void setStorage(String key, FValue value);

    static FlokAPI get() {
        RegisteredServiceProvider<FlokAPI> rsp =
            Bukkit.getServicesManager().getRegistration(FlokAPI.class);
        if (rsp == null) throw new IllegalStateException("Flok is not loaded or did not register its API.");
        return rsp.getProvider();
    }
}
