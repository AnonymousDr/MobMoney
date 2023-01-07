package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.User;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

import static com.anderhurtado.spigot.mobmoney.MobMoney.dailylimit;
import static com.anderhurtado.spigot.mobmoney.MobMoney.disableCreative;
import static com.anderhurtado.spigot.mobmoney.MobMoney.debug;
import static com.anderhurtado.spigot.mobmoney.MobMoney.disabledWorlds;
import static com.anderhurtado.spigot.mobmoney.MobMoney.bannedUUID;
import static com.anderhurtado.spigot.mobmoney.MobMoney.sendMessage;
import static com.anderhurtado.spigot.mobmoney.MobMoney.msg;
import static com.anderhurtado.spigot.mobmoney.MobMoney.dailylimitLimit;
import static com.anderhurtado.spigot.mobmoney.MobMoney.eco;
import static com.anderhurtado.spigot.mobmoney.MobMoney.spawnban;

public class EventListener implements Listener {
    @EventHandler
    public void alEntrar(PlayerJoinEvent e){
        new User(e.getPlayer().getUniqueId());
    }
    @EventHandler
    public void alSalir(PlayerQuitEvent e){
        User.getUser(e.getPlayer().getUniqueId()).disconnect();
    }
    @EventHandler
    public void alMorirENTIDAD(EntityDeathEvent e){
        LivingEntity m=e.getEntity();
        String entityType;
        if(m.getType().equals(EntityType.UNKNOWN)) entityType = MetaEntityManager.getEntityType(m);
        else entityType = m.getType().toString();
        if(debug) System.out.println("[MOBMONEY DEBUG] Entity killed: "+entityType);
        if(disabledWorlds.contains(m.getWorld().getName()))return;
        Mob mob=Mob.getEntidad(entityType);
        if(mob==null || mob.getPrice() == 0)return;
        Player j=m.getKiller();
        if(j==null) {
            if(MobMoney.crackShotConnector != null) {
                j = MobMoney.crackShotConnector.getVictim(m);
                if(j == null) return;
            } else return;
        }
        if(!j.hasPermission("mobmoney.get"))return;
        if(disableCreative&& GameMode.CREATIVE.equals(j.getGameMode()))return;
        User u=User.getUser(j.getUniqueId());
        if(bannedUUID.contains(m.getUniqueId().toString())){
            if(u.getReceiveOnDeath())sendMessage(msg.get("Events.entityBanned"),j);
            return;
        }
        UUID uuid=j.getUniqueId();
        if(!u.canGiveReward()){
            if(u.getReceiveOnDeath())sendMessage(msg.get("Events.MaxKillsReached"),j);
            return;
        }if(dailylimit!=null){
            if(dailylimit.getCount(uuid)>=dailylimitLimit){
                if(u.getReceiveOnDeath())sendMessage(msg.get("Events.dailyLimitReached").replace("%limit%",String.valueOf(dailylimitLimit)),j);
                return;
            } else dailylimit.addCount(uuid, mob.getPrice());
        }
        if(u.getReceiveOnDeath())sendMessage(msg.get("Events.hunt").replace("%entity%",mob.getName()).replace("%reward%",String.valueOf(mob.getPrice())),j);
        eco.depositPlayer(j,mob.getPrice());
    }

    @EventHandler
    public void alSpawnear(CreatureSpawnEvent e){
        if(spawnban.contains(e.getSpawnReason())){
            Entity E=e.getEntity();
            bannedUUID.add(E.getUniqueId().toString());
            try{
                for(Entity P:E.getPassengers()) bannedUUID.add(P.getUniqueId().toString());
            }catch(Throwable Ex){
                @SuppressWarnings("deprecation")
                Entity P=E.getPassenger();
                if(P!=null)bannedUUID.add(P.getUniqueId().toString());
            }
        }
    }

}
