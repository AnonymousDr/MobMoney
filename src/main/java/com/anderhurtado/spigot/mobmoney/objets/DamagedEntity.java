package com.anderhurtado.spigot.mobmoney.objets;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;

public class DamagedEntity {

    private static final HashMap<Entity, DamagedEntity> ENTITIES = new HashMap<>();

    @Nullable
    public static DamagedEntity getDamagedEntity(Entity e) {
        return ENTITIES.get(e);
    }

    @NotNull
    public static DamagedEntity getOrCreateDamagedEntity(Entity e) {
        return ENTITIES.getOrDefault(e, new DamagedEntity(e));
    }

    private HashMap<Player, Double> damages;
    private final Entity entity;
    private final CreatureSpawnEvent.SpawnReason spawnReason;
    private double damageCached;

    private DamagedEntity(Entity entity) {
        this.entity = entity;
        spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
    }

    public DamagedEntity(Entity entity, CreatureSpawnEvent.SpawnReason spawnReason) {
        this.entity = entity;
        this.spawnReason = spawnReason;
        damages = new HashMap<>();
        synchronized (ENTITIES) {
            ENTITIES.put(entity, this);
        }
    }

    public void damages(Player p, double damage) {
        if(damage == 0) return;
        if(damage > 0) {
            if(damages == null) {
                damages = new HashMap<>();
                synchronized (ENTITIES) {
                    ENTITIES.put(entity, this);
                }
            }
            damages.put(p, damages.getOrDefault(p, 0d)+damage);
        } else regenerates(-damage);
    }

    public void regenerates(double regeneration) {
        if(damages != null) {
            damages.replaceAll((p,d)->{
                if(d == null || d <= regeneration) return null;
                return d-regeneration;
            });
            for(Player p : damages.keySet()) if(damages.get(p) == null) damages.remove(p);
            if(damages.isEmpty() && spawnReason != CreatureSpawnEvent.SpawnReason.DEFAULT) remove();
        }
    }

    public void setDamageCached(double damageCached) {
        this.damageCached = damageCached;
        if(damages == null) {
            damages = new HashMap<>();
            ENTITIES.put(entity, this);
        }
    }

    public double getDamageCached() {
        return damageCached;
    }

    public void damageCachedFault(Player p) {
        if(damageCached != 0) {
            damages(p, damageCached);
            damageCached = 0;
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public CreatureSpawnEvent.SpawnReason getSpawnReason() {
        return spawnReason;
    }

    public void remove() {
        synchronized (ENTITIES) {
            ENTITIES.remove(entity, this);
        }
    }

    public double getDamageFrom(Player killer) {
        if(damages == null) return 0;
        return damages.getOrDefault(killer, 0d);
    }
}
