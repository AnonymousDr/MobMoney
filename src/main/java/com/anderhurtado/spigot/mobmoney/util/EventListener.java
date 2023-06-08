package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.ConditionalAction;
import com.anderhurtado.spigot.mobmoney.objets.DamagedEntity;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.User;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

import static com.anderhurtado.spigot.mobmoney.MobMoney.*;
import static com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent.CancelReason.PLAYER_DAILY_LIMIT_REACHED;

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
    public void onEntityDeath(EntityDeathEvent e){
        LivingEntity m=e.getEntity();
        Player j=m.getKiller();
        if(j==null) {
            if(MobMoney.crackShotConnector != null) {
                j = MobMoney.crackShotConnector.getVictim(m);
                if(j != null) {
                    DamagedEntity.getOrCreateDamagedEntity(m).damageCachedFault(j);
                }
            }

            if(j == null && myPetsConnector != null) {
                j = myPetsConnector.getPetOwner(m.getKiller());
            }
            if(j == null) return;
        }
        AsyncMobMoneyEntityKilledEvent event = new AsyncMobMoneyEntityKilledEvent(j, m, 0);
        Bukkit.getScheduler().runTaskAsynchronously(instance, ()->callEvent(event));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        User.getUser(e.getEntity().getUniqueId()).getStrikeSystem().resetStrikes();
    }

    private void callEvent(AsyncMobMoneyEntityKilledEvent e) {
        if(Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(instance, ()->callEvent(e));
            return;
        }
        LivingEntity m = e.getKilledEntity();
        DamagedEntity de = DamagedEntity.getOrCreateDamagedEntity(m);
        de.remove();
        Player j = e.getKiller();
        String entityType;
        if(m.getType().equals(EntityType.UNKNOWN)) entityType = MetaEntityManager.getEntityType(m);
        else entityType = m.getType().toString();
        if(debug) System.out.println("[MOBMONEY DEBUG] Entity killed: "+entityType);
        Mob mob=Mob.getEntity(entityType);
        UUID uuid=j.getUniqueId();
        User u=User.getUser(uuid);
        if(affectMultiplierOnPlayers || e.getKilledEntity().getType() != EntityType.PLAYER) e.setMultiplicator(u.getMultiplicator());
        else e.setMultiplicator(1);

        if(disabledWorlds.contains(m.getWorld().getName())) {
            e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.DISABLED_WORLD);
        }else if(mob == null) {
            e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.UNREGISTERED_ENTITY);
        }else {
            double reward = mob.calculateReward(j, de);
            if(e.getKilledEntity() instanceof OfflinePlayer && withdrawFromPlayers) {
                reward = Math.max(Math.min(reward, eco.getBalance((OfflinePlayer) e.getKilledEntity())),0);
                e.setWithdrawFromEntity(reward);
            }
            e.setReward(reward);
            if(e.getReward() == 0d) {
                e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.DISABLED_ENTITY);
            }else if(!j.hasPermission("mobmoney.get")) {
                e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.PLAYER_WITH_NO_PRIVILEGES);
            }else if(disableCreative&& GameMode.CREATIVE.equals(j.getGameMode())) {
                e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.CREATIVE);
            }else if(bannedUUID.contains(m.getUniqueId().toString())){
                e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.BANNED_ENTITY);
            }else if(!u.canGiveReward()){
                e.cancel(AsyncMobMoneyEntityKilledEvent.CancelReason.PLAYER_MAX_KILLS_REACHED);
            }else if(dailylimit!=null && dailylimit.getCount(uuid)>=dailylimitLimit){
                e.cancel(PLAYER_DAILY_LIMIT_REACHED);
            }
        }

        //Handles economics and strike
        int strikes = u.getStrikeSystem().addStrike(m.getType());
        ConditionalAction.handleEconomics(e, strikes);
        //Calls event
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
            double reward = Math.floor(e.getReward() * e.getMultiplicator() * 100d)/100d;
            if(dailylimit != null) dailylimit.addCount(uuid, reward);
            if(e.getKilledEntity() instanceof OfflinePlayer && e.getWithdrawFromEntity() != 0) {
                double withdraw = e.getWithdrawFromEntity();
                if(affectMultiplierOnPlayers) withdraw *= e.getMultiplicator();
                withdraw = Math.max(Math.min(withdraw, eco.getBalance((OfflinePlayer) e.getKilledEntity())),0);
                sendMessage(msg.get("Events.withdrawnByKill").replace("%player%", j.getDisplayName()).replace("%reward%",eco.format(withdraw)), e.getKilledEntity());
                eco.withdrawPlayer((OfflinePlayer)e.getKilledEntity(), withdraw);
                if(e.getReward() == e.getWithdrawFromEntity()) reward = withdraw;
            }
            if(u.getReceiveOnDeath()) {
                String mobName;
                if(mob == null) mobName = entityType;
                else mobName = mob.getName();
                sendMessage(msg.get("Events.hunt").replace("%entity%",mobName).replace("%reward%",eco.format(reward)),j);
            }
            eco.depositPlayer(j,reward);

            //Handles commands
            ConditionalAction.handleCommands(e, strikes);
        }
    }

    @EventHandler
    public void alSpawnear(CreatureSpawnEvent e){
        CreatureSpawnEvent.SpawnReason spawnReason = e.getSpawnReason();
        if(spawnban.contains(spawnReason)){
            Entity E=e.getEntity();
            bannedUUID.add(E.getUniqueId().toString());
            try{
                for(Entity P:E.getPassengers()) bannedUUID.add(P.getUniqueId().toString());
            }catch(Throwable Ex){
                @SuppressWarnings("deprecation")
                Entity P=E.getPassenger();
                if(P!=null)bannedUUID.add(P.getUniqueId().toString());
            }
        } else if(spawnReason != CreatureSpawnEvent.SpawnReason.DEFAULT) new DamagedEntity(e.getEntity(), spawnReason);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        if(e.getEntity() instanceof LivingEntity) {

            if(d instanceof Projectile) {
                ProjectileSource ps = ((Projectile) d).getShooter();
                if(ps instanceof Entity) d = (Entity) ps;
            }
            if(!(d instanceof Player)) {
                if(crackShotConnector != null) {
                    if(((LivingEntity) e.getEntity()).getHealth() > e.getFinalDamage()) {
                        Player attacker = crackShotConnector.getVictim(e.getEntity());
                        if(attacker != null) d = attacker;
                    } else { //Entidad va a morir
                        DamagedEntity.getOrCreateDamagedEntity(e.getEntity()).setDamageCached(e.getFinalDamage());
                    }
                }

                if(myPetsConnector != null) {
                    Player owner = myPetsConnector.getPetOwner((LivingEntity) d);
                    if(owner != null) d = owner;
                }
            }
            if(d instanceof Player) DamagedEntity.getOrCreateDamagedEntity(e.getEntity()).damages((Player)d, e.getFinalDamage());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRegenerate(EntityRegainHealthEvent e) {
        DamagedEntity de = DamagedEntity.getDamagedEntity(e.getEntity());
        if(de != null) de.regenerates(e.getAmount());
    }

}
