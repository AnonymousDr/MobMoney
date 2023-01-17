package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilled;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.User;
import org.bukkit.Bukkit;
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

import static com.anderhurtado.spigot.mobmoney.MobMoney.*;
import static com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilled.CancelReason.PLAYER_DAILY_LIMIT_REACHED;

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
        Player j=m.getKiller();
        if(j==null) {
            if(MobMoney.crackShotConnector != null) {
                j = MobMoney.crackShotConnector.getVictim(m);
                if(j == null) return;
            } else return;
        }
        AsyncMobMoneyEntityKilled event = new AsyncMobMoneyEntityKilled(j, m, 0);
        Bukkit.getScheduler().runTaskAsynchronously(instance, ()->callEvent(event));
    }

    private void callEvent(AsyncMobMoneyEntityKilled e) {
        if(Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(instance, ()->callEvent(e));
            return;
        }
        LivingEntity m = e.getKilledEntity();
        Player j = e.getKiller();
        String entityType;
        if(m.getType().equals(EntityType.UNKNOWN)) entityType = MetaEntityManager.getEntityType(m);
        else entityType = m.getType().toString();
        if(debug) System.out.println("[MOBMONEY DEBUG] Entity killed: "+entityType);
        Mob mob=Mob.getEntity(entityType);
        UUID uuid=j.getUniqueId();
        User u=User.getUser(uuid);

        if(disabledWorlds.contains(m.getWorld().getName())) {
            e.cancel(AsyncMobMoneyEntityKilled.CancelReason.DISABLED_WORLD);
        }else if(mob == null) {
            e.cancel(AsyncMobMoneyEntityKilled.CancelReason.UNREGISTERED_ENTITY);
        }else {
            e.setReward(mob.getPrice());
            if(mob.getPrice() == 0) {
                e.cancel(AsyncMobMoneyEntityKilled.CancelReason.DISABLED_ENTITY);
            }else if(!j.hasPermission("mobmoney.get")) {
                e.cancel(AsyncMobMoneyEntityKilled.CancelReason.PLAYER_WITH_NO_PRIVILEGES);
            }else if(disableCreative&& GameMode.CREATIVE.equals(j.getGameMode())) {
                e.cancel(AsyncMobMoneyEntityKilled.CancelReason.CREATIVE);
            }else if(bannedUUID.contains(m.getUniqueId().toString())){
                e.cancel(AsyncMobMoneyEntityKilled.CancelReason.BANNED_ENTITY);
            }else if(!u.canGiveReward()){
                e.cancel(AsyncMobMoneyEntityKilled.CancelReason.PLAYER_MAX_KILLS_REACHED);
            }else if(dailylimit!=null && dailylimit.getCount(uuid)>=dailylimitLimit){
                e.cancel(PLAYER_DAILY_LIMIT_REACHED);
            }
        }

        Bukkit.getPluginManager().callEvent(e);

        if(e.isCancelled()) {
            if(u.getReceiveOnDeath()) switch (e.getCancelReason()) {
                case BANNED_ENTITY:
                    sendMessage(msg.get("Events.entityBanned"),j);
                    break;
                case PLAYER_MAX_KILLS_REACHED:
                    sendMessage(msg.get("Events.MaxKillsReached"),j);
                    break;
                case PLAYER_DAILY_LIMIT_REACHED:
                    sendMessage(msg.get("Events.dailyLimitReached").replace("%limit%",String.valueOf(dailylimitLimit)),j);
                    break;
            }
        } else {
            if(dailylimit != null) dailylimit.addCount(uuid, e.getReward());
            if(u.getReceiveOnDeath()) {
                String mobName;
                if(mob == null) mobName = entityType;
                else mobName = mob.getName();
                sendMessage(msg.get("Events.hunt").replace("%entity%",mobName).replace("%reward%",String.valueOf(e.getReward())),j);
            }
            eco.depositPlayer(j,e.getReward());
        }
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
