package com.anderhurtado.spigot.mobmoney.objets;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class DamagedEntity {

    private static final HashMap<UUID, DamagedEntity> ENTITIES = new HashMap<UUID, DamagedEntity>() {
        @Override
        public DamagedEntity put(UUID key, DamagedEntity value) {
            clean();
            return super.put(key, value);
        }

        private long nextCheck;
        private void clean() {
            if(nextCheck < System.currentTimeMillis()) {
                long now = System.currentTimeMillis();
                nextCheck = now + 60_000;
                synchronized (ENTITIES) {
                    ENTITIES.entrySet().removeIf(m->m.getValue().expirationDate < now);
                }
            }
        }
    };

    @Nullable
    public static DamagedEntity getDamagedEntity(Entity e) {
        synchronized (ENTITIES) {
            return ENTITIES.get(e.getUniqueId());
        }
    }

    @NotNull
    public static DamagedEntity getOrCreateDamagedEntity(Entity e) {
        synchronized (ENTITIES) {
            DamagedEntity de = ENTITIES.get(e.getUniqueId());
            if(de == null) de = new DamagedEntity(e);
            return de;
        }
    }

    private HashMap<Player, Double> damages;
    private final UUID entityId;
    private double damageCached;
    private final long expirationDate;

    public DamagedEntity(Entity entity) {
        this.entityId = entity.getUniqueId();
        damages = new HashMap<>();
        synchronized (ENTITIES) {
            ENTITIES.put(entity.getUniqueId(), this);
        }
        expirationDate = System.currentTimeMillis() + 1_800_000;
    }

    public synchronized void damages(Player p, double damage) {
        if(damage == 0) return;
        if(damage > 0) {
            if(damages == null) {
                damages = new HashMap<>();
                synchronized (ENTITIES) {
                    ENTITIES.put(entityId, this);
                }
            }
            damages.put(p, damages.getOrDefault(p, 0d)+damage);
        } else regenerates(-damage);
    }

    public synchronized void regenerates(double regeneration) {
        if(damages != null) {
            damages.replaceAll((p,d)->{
                if(d == null || d <= regeneration) return 0d;
                return d-regeneration;
            });
            damages.entrySet().removeIf(e->e.getValue() == 0d);
            if(damages.isEmpty()) remove();
        }
    }

    public synchronized void setDamageCached(double damageCached) {
        this.damageCached = damageCached;
        if(damages == null) {
            damages = new HashMap<>();
            synchronized (ENTITIES) {
                ENTITIES.put(entityId, this);
            }
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

    public void remove() {
        synchronized (ENTITIES) {
            ENTITIES.remove(entityId, this);
        }
    }

    public synchronized double getDamageFrom(Player killer) {
        if(damages == null) return 0;
        return damages.getOrDefault(killer, 0d);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DamagedEntity) return ((DamagedEntity)obj).entityId.equals(entityId);
        else return false;
    }
}
