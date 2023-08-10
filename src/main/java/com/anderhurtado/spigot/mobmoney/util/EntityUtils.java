package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Method;

public class EntityUtils {

    private static Method GET_SPAWN_REASON;

    static {
        try {
            try {
                //noinspection JavaReflectionMemberAccess
                GET_SPAWN_REASON = Entity.class.getMethod("getEntitySpawnReason");
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception Ex) {
            throw new RuntimeException(Ex);
        }
    }

    public static CreatureSpawnEvent.SpawnReason getSpawnReason(Entity entity) {
        if(GET_SPAWN_REASON != null) try {
            return (CreatureSpawnEvent.SpawnReason) GET_SPAWN_REASON.invoke(entity);
        } catch (Exception ignored) {
        }
        return CreatureSpawnEvent.SpawnReason.DEFAULT;
    }

    private EntityUtils() {}
}
