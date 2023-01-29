package com.anderhurtado.spigot.mobmoney.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class AsyncMobMoneyEntityKilledEvent extends MobMoneyEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final Player killer;
    private final LivingEntity killedEntity;
    private double reward, multiplicator = 1, withdrawFromEntity;
    private CancelReason cancelReason = CancelReason.NO_CANCELED;

    public AsyncMobMoneyEntityKilledEvent(Player killer, LivingEntity killedEntity, double reward) {
        super(true);
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
    public void setWithdrawFromEntity(double withdrawFromEntity) {
        this.withdrawFromEntity = withdrawFromEntity;
    }
    public double getWithdrawFromEntity() {
        return withdrawFromEntity;
    }

    /**
     * @return Returns true if getWithdrawFromEntity() is equals to 0.
     */
    public boolean isWithdrawingFromEntity() {
        return withdrawFromEntity != 0;
    }

    public double getMultiplicator() {
        return multiplicator;
    }
    public void setMultiplicator(double multiplicator) {
        this.multiplicator = multiplicator;
    }

    /**
     * Sets the base reward
     * @return Sets the base reward
     */
    public double getReward() {
        return reward;
    }

    /**
     * Retrieves the base reward
     * @param reward Base reward
     */
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
        if(cancelReason == null) cancelReason = CancelReason.UNDEFINED;
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
