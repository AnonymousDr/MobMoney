package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.wrappedPackets.SpawnEntityWrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

public class HologramAnimation implements RewardAnimation {

    @Override
    public void apply(AsyncMobMoneyEntityKilledEvent e) {
        double reward = e.getFinalReward();

        final Location location = e.getKilledEntity().getEyeLocation();

        final SpawnEntityWrappedPacket wrappedPacket = new SpawnEntityWrappedPacket(
                EntityType.ARMOR_STAND,
                location.add(0,-1.975,0),
                new Vector(0, 0.15, 0),
                true,
                (byte) 0x20, // Invisible
                ChatColor.GREEN + MobMoney.eco.format(reward),
                true,
                false,
                null
                );
        wrappedPacket.shareSpawnPackage(location.getWorld());

        Bukkit.getScheduler().runTaskLaterAsynchronously(MobMoney.instance, ()->{
            wrappedPacket.shareDestroyPackage(location.getWorld());
        }, 60);
        MobMoney.eco.depositPlayer(e.getKiller(), reward);
    }
}
