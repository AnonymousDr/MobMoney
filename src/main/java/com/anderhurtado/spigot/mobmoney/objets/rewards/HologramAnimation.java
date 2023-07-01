package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.wrappedPackets.SpawnEntityWrappedPacket;
import com.anderhurtado.spigot.mobmoney.util.ColorManager;
import com.anderhurtado.spigot.mobmoney.util.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

public class HologramAnimation implements RewardAnimation {

    public static HologramAnimation create(ConfigurationSection cs) {
        String format = ColorManager.translateColorCodes(cs.getString("format"));
        float velocity = (float)cs.getDouble("velocity", 0);
        int ticksToDestroy = cs.getInt("tickToDestroy", 60);
        double offsetY = cs.getDouble("offsetY", 1.5);
        return new HologramAnimation(format, velocity, ticksToDestroy, offsetY);
    }

    private final float velocity;
    private final String format;
    private final int ticksToDestroy;
    private final double offsetY;

    public HologramAnimation(String format, float velocity, int ticksToDestroy, double offsetY) {
        this.velocity = velocity;
        this.format = format;
        this.ticksToDestroy = ticksToDestroy;
        this.offsetY = offsetY;
    }

    @Override
    public void apply(AsyncMobMoneyEntityKilledEvent e) {
        double reward = e.getFinalReward();

        final Location location = e.getKilledEntity().getLocation();

        final SpawnEntityWrappedPacket wrappedPacket = new SpawnEntityWrappedPacket(
                EntityType.ARMOR_STAND,
                location.add(0,offsetY,0),
                new Vector(0, velocity, 0),
                velocity != 0,
                (byte) 0x20, // Invisible
                format.replace("%money%", MobMoney.eco.format(reward)).replace("%value%", String.format("%.2f", reward)),
                true,
                false,
                legacy->{
                    Object o = (byte)0x19;
                    int index;
                    if(VersionManager.VERSION < 10) index = 10;
                    else if(VersionManager.VERSION < 14) index = 11;
                    else if(VersionManager.VERSION == 14) index = 13;
                    else if(VersionManager.VERSION < 17) index = 14;
                    else index = 15;
                    return new Object[][]{{index, o}};
                }
                );
        wrappedPacket.shareSpawnPackage(location.getWorld());

        Bukkit.getScheduler().runTaskLaterAsynchronously(MobMoney.instance, ()->wrappedPacket.shareDestroyPackage(), ticksToDestroy);
    }

    @Override
    public int getFlags() {
        return 0;
    }
}
