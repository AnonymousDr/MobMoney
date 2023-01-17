package com.anderhurtado.spigot.mobmoney.util.softdepend;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class CrackShotConnector implements Listener {

    private final HashMap<Entity, Player> attacks = new HashMap<>(); // Victim, damager

    public CrackShotConnector() {
        Bukkit.getPluginManager().registerEvents(this, MobMoney.instance);
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"Crackshot detected and connected!");
    }

    /**
     * Searches if a player has attacked an entity using CrackShot.
     * If something is found, is automatically removed from data.
     *
     * @param victim The victim affected by damage
     * @return The damager, if found
     */
    public Player getVictim(Entity victim) {
        return attacks.remove(victim);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(WeaponDamageEntityEvent e) {
        attacks.put(e.getVictim(), e.getPlayer());
    }

}
