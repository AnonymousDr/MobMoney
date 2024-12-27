package com.anderhurtado.spigot.mobmoney.util.softdepend;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.entity.Entity;

public class MythicMobsConnector5 implements MythicMobsConnector {

    MythicBukkit myMobs = MythicBukkit.inst();

    public boolean isMythicMob(Entity e) {
        return myMobs.getMobManager().isMythicMob(e);
    }

    @Nullable
    public ActiveMob getMythicMob(Entity e) {
        return myMobs.getMobManager().getMythicMobInstance(e);
    }

    public double getLevelOfMythicMob(Entity e) {
        ActiveMob am = getMythicMob(e);
        if(am == null) return 1;
        else return am.getLevel();
    }

}
