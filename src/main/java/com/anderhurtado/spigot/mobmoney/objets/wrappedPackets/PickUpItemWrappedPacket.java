package com.anderhurtado.spigot.mobmoney.objets.wrappedPackets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PickUpItemWrappedPacket {

    private static final ProtocolManager MANAGER = ProtocolLibrary.getProtocolManager();

    private final PacketContainer packetContainer = MANAGER.createPacket(PacketType.Play.Server.COLLECT);

    public PickUpItemWrappedPacket(LivingEntity le, int entityID, int stackAmount) {
        StructureModifier<Integer> integers = packetContainer.getIntegers();
        integers.write(0, entityID);
        integers.write(1, le.getEntityId());
        if(integers.size() > 2) integers.write(2, stackAmount);
    }

    public void play(Player p) {
        try{
            MANAGER.sendServerPacket(p, packetContainer);
        } catch (Exception ignored) {}
    }

    public void play(World w) {
        w.getPlayers().forEach(this::play);
    }

}
