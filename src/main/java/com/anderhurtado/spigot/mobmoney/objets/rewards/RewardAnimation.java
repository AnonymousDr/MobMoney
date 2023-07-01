package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.util.VersionManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.ArrayList;

public interface RewardAnimation {

    static RewardAnimation[] processConfig(@Nullable ConfigurationSection cs) {
        if(cs == null) return null;
        ArrayList<RewardAnimation> list = new ArrayList<>();

        RewardAnimation ra;
        for(String key : cs.getKeys(false)) {
            ra = create(cs.getConfigurationSection(key));
            if(ra != null) list.add(ra);
        }

        return list.toArray(new RewardAnimation[0]);
    }

    static RewardAnimation create(ConfigurationSection cs) {
        if(VersionManager.VERSION <= 7) return null;
        try {
            if(cs == null || !cs.getBoolean("enabled", true)) return null;
            String animationName = cs.getString("animation");
            RewardAnimation ra;
            switch (animationName.toUpperCase()) {
                case "HOLOGRAM":
                    ra = HologramAnimation.create(cs);
                    break;
                case "DROP_COINS":
                    ra = DroppedCoinsAnimation.create(cs);
                    break;
                case "DROP_ITEMS":
                    ra = DroppedItemsAnimation.create(cs);
                    break;
                default:
                    MobMoney.sendPluginMessage(ChatColor.RED+"No animation found for name: "+ChatColor.GOLD+animationName);
                    return null;
            }
            return ra;
        } catch (Exception Ex) {
            Ex.printStackTrace();
            return null;
        }
    }

    void apply(AsyncMobMoneyEntityKilledEvent e);

    /***
     * Flag definition:
     * 0x01 -> Animation manages payments
     * 0x02 -> Animation manages messages
     * @return Flag
     */
    int getFlags();

}
