package com.anderhurtado.spigot.mobmoney.objets;

import java.lang.reflect.*;
import java.util.function.BiConsumer;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class HotbarMessager {

    static BiConsumer<Entity,String> alternativa;

    /**
     * These are the Class instances. Use these to get fields or methods for
     * classes.
     */
    private static Class<?> CRAFTPLAYERCLASS, PACKET_PLAYER_CHAT_CLASS,
            ICHATCOMP, CHATMESSAGE, PACKET_CLASS, CHAT_MESSAGE_TYPE_CLASS;

    private static Field PLAYERCONNECTION;
    private static Method GETHANDLE,SENDPACKET;


    /**
     * These are the constructors for those classes. You need these to create
     * new objects.
     */
    private static Constructor<?> PACKET_PLAYER_CHAT_CONSTRUCTOR,
            CHATMESSAGE_CONSTRUCTOR;
    /**
     * Used in 1.12+. Bytes are replaced with this enum
     */
    private static Object CHAT_MESSAGE_TYPE_ENUM_OBJECT;

    static {
        try {
            // This gets the server version.
            String name = Bukkit.getServer().getClass().getName();
            name = name.substring(name.indexOf("craftbukkit.")
                    + "craftbukkit.".length());
            name = name.substring(0, name.indexOf("."));
            /**
             * This is the server version. This is how we know the server version.
             */
            String serverVersion = name;
            // This here sets the class fields.
            CRAFTPLAYERCLASS = Class.forName("org.bukkit.craftbukkit."
                    + serverVersion + ".entity.CraftPlayer");
            PACKET_PLAYER_CHAT_CLASS = Class.forName("net.minecraft.server."
                    + serverVersion + ".PacketPlayOutChat");
            PACKET_CLASS = Class.forName("net.minecraft.server."
                    + serverVersion + ".Packet");
            ICHATCOMP = Class.forName("net.minecraft.server." + serverVersion
                    + ".IChatBaseComponent");
            GETHANDLE = CRAFTPLAYERCLASS.getMethod("getHandle");
            PLAYERCONNECTION = GETHANDLE.getReturnType()
                    .getField("playerConnection");
            SENDPACKET = PLAYERCONNECTION.getType().getMethod("sendPacket", PACKET_CLASS);
            try {
                PACKET_PLAYER_CHAT_CONSTRUCTOR = PACKET_PLAYER_CHAT_CLASS
                        .getConstructor(ICHATCOMP, byte.class);
            } catch (NoSuchMethodException e) {
                CHAT_MESSAGE_TYPE_CLASS = Class.forName("net.minecraft.server."
                        + serverVersion + ".ChatMessageType");
                CHAT_MESSAGE_TYPE_ENUM_OBJECT = CHAT_MESSAGE_TYPE_CLASS
                        .getEnumConstants()[2];

                PACKET_PLAYER_CHAT_CONSTRUCTOR = PACKET_PLAYER_CHAT_CLASS
                        .getConstructor(ICHATCOMP, CHAT_MESSAGE_TYPE_CLASS);
            }

            CHATMESSAGE = Class.forName("net.minecraft.server."
                    + serverVersion + ".ChatMessage");

            CHATMESSAGE_CONSTRUCTOR = CHATMESSAGE.getConstructor(
                    String.class, Object[].class);
        }catch (Exception e) {
            alternativa=(j,msg)->{
                if(j instanceof Player) ((Player)j).spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(msg));
                else j.sendMessage(msg);
            };
        }
    }

    /**
     * Sends the hotbar message 'message' to the player 'player'
     *
     * @param player
     * @param message
     */
    public static void sendHotBarMessage(Entity player, String message) {
        if(alternativa!=null)alternativa.accept(player,message);
        else try {
            // This creates the IChatComponentBase instance
            Object icb = CHATMESSAGE_CONSTRUCTOR.newInstance(message,
                    new Object[0]);
            // This creates the packet
            Object packet;
            try {
                packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb,
                        (byte) 2);
            } catch (Exception e) {
                packet = PACKET_PLAYER_CHAT_CONSTRUCTOR.newInstance(icb,
                        CHAT_MESSAGE_TYPE_ENUM_OBJECT);
            }
            // This casts the player to a craftplayer
            Object craftplayerInst = CRAFTPLAYERCLASS.cast(player);
            // This invokes the method above.
            Object methodhHandle = GETHANDLE.invoke(craftplayerInst);
            // This gets the player's connection
            Object playerConnection = PLAYERCONNECTION.get(methodhHandle);
            // This sends the packet.
            SENDPACKET
                    .invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}