package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.util.ItemStackUtils;
import com.anderhurtado.spigot.mobmoney.util.MaxHealth;
import com.anderhurtado.spigot.mobmoney.util.PreDefinedExpression;
import net.objecthunter.exp4j.Expression;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class DroppedItemsAnimation implements RewardAnimation {

    public static DroppedItemsAnimation create(ConfigurationSection cs) {
        Expression damageRequired = new PreDefinedExpression(cs.getString("damageRequired", "0")).variable("maxHealth").build();
        boolean recollectableByEveryone = cs.getBoolean("recollectableByEveryone", true);
        ItemStack[] items = ItemStackUtils.convert(cs.getStringList("drops").toArray(new String[0]));
        return new DroppedItemsAnimation(damageRequired, recollectableByEveryone, items);
    }

    private final Expression damageRequiredExpression;
    private final boolean recollectableByEveryone;
    private final ItemStack[] items;

    public DroppedItemsAnimation(Expression damageRequiredExpression, boolean recollectableByEveryone, ItemStack... items) {
        this.damageRequiredExpression = damageRequiredExpression;
        this.recollectableByEveryone = recollectableByEveryone;
        this.items = items;
    }

    @Override
    public void apply(AsyncMobMoneyEntityKilledEvent e) {
        double requiredDamage;
        synchronized (damageRequiredExpression) {
            damageRequiredExpression.setVariable("maxHealth", MaxHealth.getMaxHealth(e.getKilledEntity()));
            requiredDamage = damageRequiredExpression.evaluate();
        }
        if(e.getDamagedEntity().getDamageFrom(e.getKiller()) < requiredDamage) return;
        spawnItems(e.getKilledEntity().getLocation(), e.getKiller());
    }

    private void spawnItems(Location location, Player killer) {
        if(!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().callSyncMethod(MobMoney.instance, ()->{
                spawnItems(location, killer);
                return null;
            });
            return;
        }
        Item drop;
        for(ItemStack item:items) {
            drop = location.getWorld().dropItemNaturally(location, item);
            if(!recollectableByEveryone) new DroppedItem(killer, drop);
        }
    }

    @Override
    public int getFlags() {
        return 0;
    }
}
class DroppedItem implements Listener {

    static GetListener pickUpItemListener;

    static {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            pickUpItemListener = NewAntiPickUp::new;
        } catch (Throwable ignored) {
            pickUpItemListener = LegacyAntiPickUp::new;
        }
    }

    final Player receiver;
    final Item item;

    DroppedItem(Player receiver, Item item) {
        this.receiver = receiver;
        this.item = item;
        Bukkit.getPluginManager().registerEvents(this, MobMoney.instance);
        Bukkit.getPluginManager().registerEvents(pickUpItemListener.get(this), MobMoney.instance);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickUp(InventoryPickupItemEvent e) {
        if(e.getItem().equals(item)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDestroy(ItemDespawnEvent e) {
        if(e.getEntity().equals(item)) HandlerList.unregisterAll(this);
    }
}
interface GetListener {
    Listener get(DroppedItem drop);
}
class NewAntiPickUp implements Listener {

    private DroppedItem drop;

    NewAntiPickUp (DroppedItem drop) {
        this.drop = drop;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickUp(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if(!e.getItem().equals(drop.item)) return;
        if(!e.getEntity().equals(drop.receiver)) {
            e.setCancelled(true);
            return;
        }
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(drop);
    }
}

class LegacyAntiPickUp implements Listener {

    private DroppedItem drop;

    LegacyAntiPickUp(DroppedItem drop) {
        this.drop = drop;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickUp(org.bukkit.event.player.PlayerPickupItemEvent e) {
        if(!e.getItem().equals(drop.item)) return;
        if(!e.getPlayer().equals(drop.receiver)) {
            e.setCancelled(true);
            return;
        }
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(drop);
    }

}