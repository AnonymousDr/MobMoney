package com.anderhurtado.spigot.mobmoney.objets.wrappedPackets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.org.eclipse.sisu.Nullable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public class SpawnEntityWrappedPacket {

    private static final ProtocolManager MANAGER = ProtocolLibrary.getProtocolManager();

    private final PacketContainer entity, metadata, kill, move;
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
     * @param metadataModifier Consumer to modify the metadata before sharing the packet
     */
    public SpawnEntityWrappedPacket(EntityType et, Location location, @Nullable Vector velocity, boolean constantVelocity, @Nullable byte data, @Nullable String name, boolean silent, boolean gravity, @Nullable Consumer<WrappedDataWatcher> metadataModifier) {
        uuid = UUID.randomUUID();
        intId = (int) uuid.getMostSignificantBits();
        entity = MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        entity.getIntegers().write(0, intId);
        entity.getUUIDs().write(0, uuid);
        entity.getEntityTypeModifier().write(0, et);
        entity.getDoubles().write(0, location.getX());
        entity.getDoubles().write(1, location.getY());
        entity.getDoubles().write(2, location.getZ());
        entity.getIntegers().write(1, (int) (location.getPitch() * 256F / 360F));
        entity.getIntegers().write(2, (int) (location.getYaw() * 256F / 360F));
        if(velocity != null) {
            entity.getIntegers().write(3, (int) (velocity.getX() * 204.8));
            entity.getIntegers().write(4, (int) (velocity.getY() * 204.8));
            entity.getIntegers().write(5, (int) (velocity.getZ() * 204.8));
        }

        metadata = MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, intId);

        /* /**
        EasyMetadataPacket emp = new EasyMetadataPacket(null);
        emp.write(0, data);
        if(name != null) emp.writeOptional(2, WrappedChatComponent.fromChatMessage(name)[0]);
        //if(name != null) emp.writeOptional(2, name);
        emp.write(3, name != null);
        emp.write(4, silent);
        emp.write(5, gravity);

        metadata.getWatchableCollectionModifier().write(0, emp.export());
        /*
        ArrayList<WrappedDataValue> dataValues = new ArrayList<>();
        dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), data));
        if(name != null) dataValues.add(new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), WrappedChatComponent.fromChatMessage(name)[0].getHandle()));
        dataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), name != null));
        dataValues.add(new WrappedDataValue(4, WrappedDataWatcher.Registry.get(Boolean.class), silent));
        dataValues.add(new WrappedDataValue(5, WrappedDataWatcher.Registry.get(Boolean.class), gravity));
        metadata.getDataValueCollectionModifier().write(0, dataValues);/**/
    /* /**/
        WrappedDataWatcher wdw = new WrappedDataWatcher();
        wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), data);
        if(name != null) wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), name == null? Optional.empty() : Optional.of(WrappedChatComponent.fromChatMessage(name)[0].getHandle()));
        wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), name != null);
        wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(4, WrappedDataWatcher.Registry.get(Boolean.class)), silent);
        wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class)), gravity);

        if(metadataModifier != null) metadataModifier.accept(wdw);

        metadata.getWatchableCollectionModifier().write(0, wdw.getWatchableObjects());
    /**/

        if(velocity != null && constantVelocity) {
            move = MANAGER.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE);
            move.getIntegers().write(0, intId);
            move.getShorts().write(0, (short) (velocity.getX() * 204.8));
            move.getShorts().write(1, (short) (velocity.getY() * 204.8));
            move.getShorts().write(2, (short) (velocity.getZ() * 204.8));
            move.getBooleans().write(0, false);
        } else move = null;

        kill = MANAGER.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        kill.getIntegerArrays().write(0, new int[]{intId});
    }

    public void shareSpawnPackage(Player p) {
        try {
            MANAGER.sendServerPacket(p, entity);
            MANAGER.sendServerPacket(p, metadata);
            if(move != null && moveTask == null) {
                final World w = p.getWorld();
                moveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MobMoney.instance, ()->shareMovementPackage(w), 1, 1);
            }
        } catch (Exception ignored) {
        }
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

    public void shareSpawnPackage(World w) {
        w.getPlayers().forEach(this::shareSpawnPackage);
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
