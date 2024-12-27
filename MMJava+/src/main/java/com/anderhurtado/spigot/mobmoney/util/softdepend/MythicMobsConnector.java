package com.anderhurtado.spigot.mobmoney.util.softdepend;

import org.bukkit.entity.Entity;

public interface MythicMobsConnector {

    public static MythicMobsConnector getInstance() {
        try {
            return new MythicMobsConnector5();
        } catch (Throwable T) {
            return new MythicMobsConnector4();
        }
    }

    boolean isMythicMob(Entity e);
    double getLevelOfMythicMob(Entity e);

}
