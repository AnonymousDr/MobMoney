package com.anderhurtado.spigot.mobmoney.util.softdepend;

import dev.aurelium.auramobs.AuraMobs;
import dev.aurelium.auramobs.api.AuraMobsAPI;
import org.bukkit.entity.LivingEntity;

public class AuraMobsConnector {

    public int getLevel(LivingEntity le) {
        try {
            return AuraMobsAPI.getMobLevel(le);
        } catch (IllegalArgumentException IAEx) {
            AuraMobs plugin = AuraMobs.getPlugin(AuraMobs.class);
            if (!plugin.isAuraMob(le)) {
                return 1;
            }
            Integer persistent = le.getPersistentDataContainer().get(plugin.getMobKey(), org.bukkit.persistence.PersistentDataType.INTEGER);
            if (persistent == null) {
                return 1;
            }
            return persistent;
        }
    }

}
