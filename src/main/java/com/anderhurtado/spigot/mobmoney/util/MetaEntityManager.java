package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

public class MetaEntityManager {

    private static final String VERSION;

    private static final Method ENTITY_HANDLE;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = version.substring(version.lastIndexOf('.') + 1);

        try{
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + VERSION +".entity.CraftEntity");
            ENTITY_HANDLE = clazz.getMethod("getHandle");
        } catch (Throwable Th) {
            throw new RuntimeException(Th);
        }
    }

    private MetaEntityManager() {}

    public static String getEntityType(Entity entity) {
        try{
            String entityData = ENTITY_HANDLE.invoke(entity).toString();
            return entityData.substring(0, entityData.indexOf('[')).replace('.', '_');
        } catch (Throwable Th) {
            return "UNKNOWN";
        }
    }

}
