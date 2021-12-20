package com.anderhurtado.spigot.mobmoney.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

public class MetaEntityManager {

    private static final String VERSION;

    private static Method ENTITY_HANDLE, ENTITY_HANDLE_GET_TYPE;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = version.substring(version.lastIndexOf('.') + 1);

        try{
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + VERSION +".entity.CraftEntity");
            ENTITY_HANDLE = clazz.getMethod("getHandle");

            clazz = Class.forName("net.minecraft.server."+VERSION+".Entity");
            ENTITY_HANDLE_GET_TYPE = clazz.getMethod("getEntityType");
        } catch (Throwable Th) {
            throw new RuntimeException(Th);
        }
    }

    private MetaEntityManager() {}

    public static String getEntityType(Entity entity) {
        try{
            Object entityNative = ENTITY_HANDLE.invoke(entity);
            return ENTITY_HANDLE_GET_TYPE.invoke(entityNative).toString().replace('.', '_');
        } catch (Throwable Th) {
            return "UNKNOWN";
        }
    }

}
