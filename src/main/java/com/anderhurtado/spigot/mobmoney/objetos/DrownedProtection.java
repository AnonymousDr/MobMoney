package com.anderhurtado.spigot.mobmoney.objetos;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;

public class DrownedProtection implements Listener{

    HashMap<Entity,String> entidades=new HashMap<>();

    @EventHandler
    public void alSpawnearEntidad(CreatureSpawnEvent e){
        if(!e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.DROWNED))return;
        Entity E=e.getEntity();
        Location l=E.getLocation();
        for(Entity en:E.getNearbyEntities(0,0,0)){
            if(!en.getType().equals(EntityType.ZOMBIE))continue;
            if(!MobMoney.bannedUUID.contains(en.getUniqueId().toString()))continue;
            MobMoney.bannedUUID.remove(en.getUniqueId().toString());
            MobMoney.bannedUUID.add(E.getUniqueId().toString());
            break;
        }
    }
}
