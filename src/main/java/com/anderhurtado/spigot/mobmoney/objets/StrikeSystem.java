package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import org.bukkit.entity.EntityType;

import java.util.HashMap;

public class StrikeSystem {

    private static final HashMap<EntityType, Integer> MAX_TIME = new HashMap<>();

    /**
     * Sets the max time of entities.
     * @param et EntityType. UNKNOWN as default.
     * @param time Max time to pass in miliseconds.
     */
    public static void setMaxTime(EntityType et, int time) {
        if(et == null) et = EntityType.UNKNOWN;
        MAX_TIME.put(et, time);
    }

    /**
     * Gets the max time of the strike. If not registered, will get the default value.
     * The user can define their own default value, but the default time defined by code is 5000 miliseconds.
     * @param et Entity type.
     * @return Time in ticks.
     */
    public static int getMaxTime(EntityType et) {
        if(MAX_TIME.containsKey(et)) return MAX_TIME.get(et);
        else return MAX_TIME.getOrDefault(EntityType.UNKNOWN, 5000);
    }

    private final HashMap<EntityType, Integer> strikes = new HashMap<>();
    private final HashMap<EntityType, Long> lastKill = new HashMap<>();

    /**
     * Adds an strike computing the last time that the user has killed that entity.
     * @param et EntityType.
     * @return Number of strikes after computing.
     */
    public int addStrike(EntityType et) {
        long time = System.currentTimeMillis();
        if(lastKill.getOrDefault(et, time) + getMaxTime(et) < time) resetStrikes(et);
        int kills;
        if(strikes.containsKey(et)) strikes.replace(et, kills = strikes.get(et)+1);
        else strikes.put(et, kills = 1);
        lastKill.put(et, time);
        if(MobMoney.debug) System.out.println("[MOBMONEY DEBUG] New strike: "+kills);
        return kills;
    }

    /**
     * Computes the last time that the user has killed that entity.
     * @param et EntityType.
     * @return Number of strikes after computing.
     */
    public int getStrikes(EntityType et) {
        long time = System.currentTimeMillis();
        if(lastKill.getOrDefault(et, time) < time + getMaxTime(et)) resetStrikes(et);
        return strikes.getOrDefault(et, 0);
    }

    public void resetStrikes(EntityType et) {
        strikes.remove(et);
        lastKill.remove(et);
    }

    public void resetStrikes() {
        strikes.clear();
        lastKill.clear();
    }

}
