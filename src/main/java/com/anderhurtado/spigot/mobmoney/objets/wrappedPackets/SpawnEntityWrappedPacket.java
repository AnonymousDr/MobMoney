package com.anderhurtado.spigot.mobmoney.objets.wrappedPackets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.util.VersionManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SpawnEntityWrappedPacket {

    private static final ProtocolManager MANAGER = ProtocolLibrary.getProtocolManager();

    public static void transform(WrappedDataWatcher wdw, int index, Object value) {
        WrappedWatchableObject wwo = wdw.getWatchableObject(index);
        if(wwo == null) wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, WrappedDataWatcher.Registry.get(value.getClass())), value);
        else wwo.setValue(value);
    }

    private final PacketContainer entity, kill, move, metadata;
    private final UUID uuid;
    private final int intId;
    private BukkitTask moveTask;

    /**
     * Creates entity wrapped. This allows spawning, edit some metadata, and destroy the entity
     *
     * @param et EntityType
     * @param location Location of virtual entity
     * @param velocity Constant velocity
     * @param data byte with some bits of data:
     *             0x01	Is on fire
     *             0x02	Is crouching
     *             0x04	Unused (previously riding)
     *             0x08	Is sprinting
     *             0x10	Is swimming
     *             0x20	Is invisible
     *             0x40	has glowing effect
     *             0x80	Is flying with an elytra
     * @param name Name of the entity (nullable)
     * @param silent is silent?
     * @param gravity has gravity?
     * @param datas Special data[][index,value]
     */
    public SpawnEntityWrappedPacket(EntityType et, Location location, @Nullable Vector velocity, boolean constantVelocity, @Nullable byte data, @Nullable String name, boolean silent, boolean gravity, @Nullable Function<Boolean, Object[][]> datas) {
        boolean legacy;
        uuid = UUID.randomUUID();
        intId = (int) uuid.getMostSignificantBits();
        entity = MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        entity.getIntegers().write(0, intId);
        if(entity.getUUIDs().size() > 0) entity.getUUIDs().write(0, uuid);
        if(entity.getEntityTypeModifier().size() == 0) {
            legacy = true;
            int entityId;
            switch (et) {
                case DROPPED_ITEM:
                    entityId = 2;
                    break;
                case ARMOR_STAND:
                    entityId = 0x4E;
                    break;
                default:
                    entityId = et.getTypeId();
                    break;
            }
            if(entity.getDoubles().size() >= 3) {
                entity.getIntegers().write(6, entityId);
                entity.getDoubles().write(0, location.getX());
                entity.getDoubles().write(1, location.getY());
                entity.getDoubles().write(2, location.getZ());
            } else {
                entity.getIntegers().write(9, entityId);
                entity.getIntegers().write(1, (int) (location.getX() * 32));
                entity.getIntegers().write(2, (int) (location.getY() * 32));
                entity.getIntegers().write(3, (int) (location.getZ() * 32));
            }
        }
        else {
            legacy = false;
            entity.getEntityTypeModifier().write(0, et);
            entity.getDoubles().write(0, location.getX());
            entity.getDoubles().write(1, location.getY());
            entity.getDoubles().write(2, location.getZ());
        }

        metadata = MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, intId);

        WrappedDataWatcher wdw;
        if(legacy) {
            try {
                wdw = Bukkit.getScheduler().callSyncMethod(MobMoney.instance, ()->{
                    Entity sample = location.getWorld().spawnEntity(new Location(location.getWorld(), 1, -10, 1), et);
                    if(sample instanceof ArmorStand) {
                        ArmorStand as = ((ArmorStand)sample);
                        as.setGravity(gravity);
                        if(name != null) {
                            as.setCustomName(name);
                            as.setCustomNameVisible(true);
                        }
                    }
                    WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(sample);
                    sample.remove();
                    return dataWatcher;
                }).get().deepClone();
            } catch (Exception Ex) {
                throw new RuntimeException(Ex);
            }
            for(WrappedWatchableObject wwo:wdw.getWatchableObjects()) {
                Object value = wwo.getValue();
            }
            transform(wdw, 0, data);
            if(datas != null) for (Object[] objects : datas.apply(true)) transform(wdw, (int) objects[0], objects[1]);
        } else {
            if(VersionManager.VERSION >= 19) {
                wdw = null;
                List<WrappedDataValue> list = new ArrayList<>();
                list.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), data));
                list.add(new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), name == null? Optional.empty() : Optional.of(WrappedChatComponent.fromChatMessage(name)[0].getHandle())));
                list.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), name != null));
                list.add(new WrappedDataValue(4, WrappedDataWatcher.Registry.get(Boolean.class), silent));
                list.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), gravity));
                if(datas != null) for (Object[] objects : datas.apply(false)) list.add(new WrappedDataValue((int)objects[0], WrappedDataWatcher.Registry.get(objects[1].getClass()), objects[1]));
                metadata.getDataValueCollectionModifier().write(0, list);
            } else {
                wdw = new WrappedDataWatcher();
                wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), data);
                wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), name == null? Optional.empty() : Optional.of(WrappedChatComponent.fromChatMessage(name)[0].getHandle()));
                wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), name != null);
                wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(4, WrappedDataWatcher.Registry.get(Boolean.class)), silent);
                wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class)), gravity);
                if(datas != null) for (Object[] objects : datas.apply(false)) transform(wdw, (int) objects[0], objects[1]);
            }
        }

        if(wdw != null) metadata.getWatchableCollectionModifier().write(0, wdw.getWatchableObjects());

        if(velocity != null && constantVelocity) {
            move = MANAGER.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE);
            move.getIntegers().write(0, intId);
            if(move.getBytes().size() >= 3) {
                move.getBytes().write(0, (byte) Math.round(velocity.getX() * 32));
                move.getBytes().write(1, (byte) Math.round(velocity.getY() * 32));
                move.getBytes().write(2, (byte) Math.round(velocity.getZ() * 32));
            } else if(move.getShorts().size() >= 3){
                move.getShorts().write(0, (short) Math.round(velocity.getX() * 8000));
                move.getShorts().write(1, (short) Math.round(velocity.getY() * 8000));
                move.getShorts().write(2, (short) Math.round(velocity.getZ() * 8000));
            } else {
                move.getIntegers().write(1, (int) Math.round(velocity.getX() * 8000));
                move.getIntegers().write(2, (int) Math.round(velocity.getY() * 8000));
                move.getIntegers().write(3, (int) Math.round(velocity.getZ() * 8000));
            }
            move.getBooleans().write(0, false);
        } else move = null;

        kill = MANAGER.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        if(kill.getIntegerArrays().size() == 0) kill.getIntLists().write(0, Collections.singletonList(intId));
        else kill.getIntegerArrays().write(0, new int[]{intId});
    }


    /**
     * Shares spawn package to a player. This is executed always with Bukkit's primary thread. If is not primary thread
     * makes a sync call
     * @param p Player
     */
    public void shareSpawnPackage(Player p) {
        if(!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().callSyncMethod(MobMoney.instance,()->{
                shareSpawnPackage(p);
                return null;
            });
        } else try {
            MANAGER.sendServerPacket(p, entity);
            MANAGER.sendServerPacket(p, metadata);
            if(move != null && moveTask == null) {
                final World w = p.getWorld();
                moveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MobMoney.instance, ()->shareMovementPackage(w), 1, 1);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Shares spawn package world-wide. This is executed always with Bukkit's primary thread. If is not primary thread
     * makes a sync call
     * @param w World
     */
    public void shareSpawnPackage(World w) {
        if(!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().callSyncMethod(MobMoney.instance, ()->{
                shareSpawnPackage(w);
                return null;
            });
        } else w.getPlayers().forEach(this::shareSpawnPackage);
    }

    void shareMovementPackage(Player p) {
        try{
            MANAGER.sendServerPacket(p, move);
        } catch (Exception ignored) {
        }
    }

    void shareMovementPackage(World w) {
        w.getPlayers().forEach(this::shareMovementPackage);
    }

    public void shareDestroyPackage(Player p ) {
        if(moveTask != null) {
            moveTask.cancel();
            moveTask = null;
        }
        try {
            MANAGER.sendServerPacket(p, kill);
        } catch (Exception ignored) {
        }
    }

    public void shareDestroyPackage(World w) {
        w.getPlayers().forEach(this::shareDestroyPackage);
    }

    public void shareDestroyPackage() {
        Bukkit.getOnlinePlayers().forEach(this::shareDestroyPackage);
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getEntityIntID() {
        return intId;
    }

    public PacketContainer getSpawnPacket() {
        return entity;
    }

    public PacketContainer getMetadataPacket() {
        return metadata;
    }

    public PacketContainer getKillPacket() {
        return kill;
    }

    public PacketContainer getMovePacket() {
        return move;
    }

}
