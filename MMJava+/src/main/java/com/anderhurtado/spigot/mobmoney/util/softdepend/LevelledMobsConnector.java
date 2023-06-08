package com.anderhurtado.spigot.mobmoney.util.softdepend;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;

public class LevelledMobsConnector {

    public int getLevel(LivingEntity e) {
        return LevelledMobs.getInstance().levelInterface.getLevelOfMob(e);
    }
}
