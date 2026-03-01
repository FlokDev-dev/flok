package wbog.flok.api;

import org.bukkit.entity.Player;

public interface FlokContext {

    Player getPlayer();

    FValue getLocal(String name);

    void setLocal(String name, FValue value);

    default boolean hasLocal(String name) {
        return !getLocal(name).isNull();
    }


    void cancelEvent();

    boolean isEventCancelled();
}