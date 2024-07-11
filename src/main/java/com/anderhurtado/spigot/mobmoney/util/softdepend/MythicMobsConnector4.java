package com.anderhurtado.spigot.mobmoney.util.softdepend;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.BukkitAPIHelper;
import org.bukkit.entity.Entity;

public class MythicMobsConnector4 implements MythicMobsConnector {

    BukkitAPIHelper myMobs = MythicMobs.getPlugin(MythicMobs.class).getAPIHelper();

    @Override
    public boolean isMythicMob(Entity e) {
        return myMobs.isMythicMob(e);
    }

    @Override
    public double getLevelOfMythicMob(Entity e) {
        return myMobs.getMythicMobInstance(e).getLevel();
    }
}
