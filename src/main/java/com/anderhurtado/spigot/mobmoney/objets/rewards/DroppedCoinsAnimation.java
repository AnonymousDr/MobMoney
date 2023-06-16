package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.wrappedPackets.SpawnEntityWrappedPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class DroppedCoinsAnimation implements RewardAnimation {

    static {
        try{
            String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            String NMS_BASE = "net.minecraft.server." + NMS_VERSION + ".";
            Class<?> IBlockData = Class.forName(NMS_BASE.concat("IBlockData"));
            GET_COMBINED_ID = Class.forName(NMS_BASE.concat("Block")).getMethod("getCombinedId", IBlockData);
            GET_STATE = Class.forName("org.bukkit.craftbukkit."+NMS_VERSION+".block.data.CraftBlockData").getMethod("getState");
            //ITEMSTACK_GET_HANDLER = Class.forName("org.bukkit.craftbukkit."+NMS_VERSION+".inventory.CraftItemStack").getMethod(((net.minecraft.server.v1_16_R3.ItemStack)null).º)
                    ;
        } catch (Throwable T) {
            throw new RuntimeException(T);
        }
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener() {

            List<Integer> list = new ArrayList<>();
            @Override
            public void onPacketSending(PacketEvent packetEvent) {
                if(packetEvent.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    list.add(packetEvent.getPacket().getIntegers().read(0));
                } else {

                    if(list.contains(packetEvent.getPacket().getIntegers().read(0))) {
                        PacketContainer pc = packetEvent.getPacket();
                        try{
                            for(Method m : pc.getClass().getDeclaredMethods()) {
                                if(m.getParameterCount() > 0) continue;
                                if(!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) continue;
                                if(m.getName().equalsIgnoreCase("getProtocols")) continue;
                                Object o;
                                try{
                                    o = m.invoke(pc);
                                } catch (Throwable T) {
                                    System.out.println(m.getName()+": ERROR");
                                    continue;
                                }
                                if(!(o instanceof StructureModifier<?>)) continue;
                                StructureModifier<?> sm = (StructureModifier<?>) o;
                                if(sm.size() == 0) continue;
                                System.out.println(m.getName());
                                int size = sm.size();
                                for(int i=0; i<size; i++) {
                                    System.out.println(i+"/"+size+" = "+sm.read(i));
                                }
                            }
                        } catch (Throwable T) {
                            T.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onPacketReceiving(PacketEvent packetEvent) {

            }

            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return ListeningWhitelist.newBuilder().types(PacketType.Play.Server.SPAWN_ENTITY, PacketType.Play.Server.ENTITY_METADATA).build();
            }

            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return ListeningWhitelist.EMPTY_WHITELIST;
            }

            @Override
            public Plugin getPlugin() {
                return MobMoney.instance;
            }
        });
    }

    private static final Method GET_COMBINED_ID, GET_STATE;//, ITEMSTACK_GET_HANDLER;

    private static int getCombinedId(Material m) {
        try {
            return (int) GET_COMBINED_ID.invoke(null, GET_STATE.invoke(m.createBlockData()));
        } catch (Throwable T) {
            throw new RuntimeException(T);
        }
    }

    @Override
    public void apply(AsyncMobMoneyEntityKilledEvent e) {
        double reward = e.getFinalReward();
        final Location location = e.getKilledEntity().getEyeLocation();

        final SpawnEntityWrappedPacket wrappedPacket = new SpawnEntityWrappedPacket(
                EntityType.DROPPED_ITEM,
                location.add(0,0,0),
                null,
                false,
                (byte) 0x40,
                ChatColor.GREEN + MobMoney.eco.format(reward),
                false,
                true,
                wdw->wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(14, WrappedDataWatcher.Registry.get(Integer.class)), getCombinedId(Material.PLAYER_HEAD))
        );
        //wrappedPacket.getSpawnPacket().getModifier().write(11, new ItemStack(Material.STONE, 2))
        wrappedPacket.getSpawnPacket().getIntegers().write(2, 1);
        wrappedPacket.shareSpawnPackage(e.getKiller().getWorld());
    }
}
