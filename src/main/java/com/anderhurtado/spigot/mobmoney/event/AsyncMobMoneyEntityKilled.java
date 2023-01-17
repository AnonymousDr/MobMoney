package com.anderhurtado.spigot.mobmoney.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class AsyncMobMoneyEntityKilled extends MobMoneyEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final Player killer;
    private final LivingEntity killedEntity;
    private double reward;
    private CancelReason cancelReason;

    public AsyncMobMoneyEntityKilled(Player killer, LivingEntity killedEntity, double reward) {
        this.killer = killer;
        this.killedEntity = killedEntity;
        this.reward = reward;
    }

    public Player getKiller() {
        return killer;
    }

    public LivingEntity getKilledEntity() {
        return killedEntity;
    }

    public double getReward() {
        return reward;
    }
    public void setReward(double reward) {
        this.reward = reward;
    }

    @Override
    public boolean isCancelled() {
        return cancelReason != CancelReason.NO_CANCELED;
    }

    public CancelReason getCancelReason() {
        return cancelReason;
    }

    @Override
    public void setCancelled(boolean b) {
        if(b) cancel();
        else uncancel();
    }

    public void cancel() {
        cancel(CancelReason.UNDEFINED);
    }

    public void cancel(CancelReason cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void uncancel() {
        cancelReason = CancelReason.NO_CANCELED;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public enum CancelReason {
        NO_CANCELED, UNDEFINED, UNREGISTERED_ENTITY, DISABLED_ENTITY, DISABLED_WORLD, PLAYER_WITH_NO_PRIVILEGES, CREATIVE, BANNED_ENTITY, PLAYER_MAX_KILLS_REACHED, PLAYER_DAILY_LIMIT_REACHED
    }
}
