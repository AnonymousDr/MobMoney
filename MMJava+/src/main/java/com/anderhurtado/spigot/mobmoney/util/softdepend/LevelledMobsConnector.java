package com.anderhurtado.spigot.mobmoney.util.softdepend;

import org.bukkit.entity.LivingEntity;

public class LevelledMobsConnector {

    public int getLevel(LivingEntity e) {
        try {
            return me.lokka30.levelledmobs.LevelledMobs.getInstance().levelInterface.getLevelOfMob(e);
        } catch (NoClassDefFoundError NCDFEr) {
            return io.github.arcaneplugins.levelledmobs.LevelledMobs.getInstance().getLevelInterface().getLevelOfMob(e);
        }
    }
}
