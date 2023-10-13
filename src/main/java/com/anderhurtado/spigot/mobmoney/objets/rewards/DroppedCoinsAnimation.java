package com.anderhurtado.spigot.mobmoney.objets.rewards;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.CounterByObject;
import com.anderhurtado.spigot.mobmoney.objets.DefinedSound;
import com.anderhurtado.spigot.mobmoney.objets.EntityBox;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.wrappedPackets.PickUpItemWrappedPacket;
import com.anderhurtado.spigot.mobmoney.objets.wrappedPackets.SpawnEntityWrappedPacket;
import com.anderhurtado.spigot.mobmoney.util.*;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import net.objecthunter.exp4j.Expression;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;

import static com.anderhurtado.spigot.mobmoney.MobMoney.eco;

public class DroppedCoinsAnimation implements RewardAnimation {

    private static final ProtocolManager MANAGER = ProtocolLibrary.getProtocolManager();
    private static final HashMap<Long, AnimationInstance> ANIMATION_INSTANCES = new HashMap<>();
    private static final ItemStack INTERNAL_BOOK;
    private static final BookMeta INTERNAL_BOOK_META;
    private static long ITEM_SUBID = 0;

    static {
        INTERNAL_BOOK = new ItemStack(Material.WRITTEN_BOOK);
        INTERNAL_BOOK_META = (BookMeta) INTERNAL_BOOK.getItemMeta();
        assert INTERNAL_BOOK_META != null;
        INTERNAL_BOOK_META.setPages("INTERNAL BOOK. PLEASE DESTROY", "INTERNAL BOOK. PLEASE DESTROY", "INTERNAL BOOK. PLEASE DESTROY");
        INTERNAL_BOOK_META.setAuthor("Mob Money plugin by Anonymous_Dr");
        INTERNAL_BOOK_META.setLore(Collections.singletonList("INTERNAL BOOK. DESTROY"));
        INTERNAL_BOOK_META.setTitle("MOBMONEY INTERNAL OBJECT");
        INTERNAL_BOOK_META.setDisplayName("MOBMONEY INTERNAL OBJECT");
        try{
            INTERNAL_BOOK_META.setGeneration(BookMeta.Generation.TATTERED);
        } catch (Throwable ignored) {
        }
        INTERNAL_BOOK.setItemMeta(INTERNAL_BOOK_META);
        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
            public void onMerge(ItemMergeEvent e) {
                if(isInternal(e.getEntity().getItemStack())) {
                    e.setCancelled(true);
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
            public void onInventoryPickUp(InventoryPickupItemEvent e) {
                if(isInternal(e.getItem().getItemStack())) e.setCancelled(true);
            }

        }, MobMoney.instance);
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
                public void onPickUp(org.bukkit.event.entity.EntityPickupItemEvent e) {
                    if(isInternal(e.getItem().getItemStack())) e.setCancelled(true);
                }

            }, MobMoney.instance);
        } catch (Throwable ignored) {
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
                public void onPickUp(org.bukkit.event.player.PlayerPickupItemEvent e) {
                    if(isInternal(e.getItem().getItemStack())) e.setCancelled(true);
                }

            }, MobMoney.instance);
        }
        MANAGER.addPacketListener(new PacketListener() {
            @Override
            public void onPacketSending(PacketEvent packetEvent) {
                PacketType pt = packetEvent.getPacketType();
                if(pt.equals(PacketType.Play.Server.WINDOW_ITEMS)) {
                    onWindowUpdate(packetEvent);
                } else if(pt.equals(PacketType.Play.Server.SET_SLOT)) {
                    onSlotSet(packetEvent);
                } else if(pt.equals(PacketType.Play.Server.ENTITY_METADATA)) {
                    onEntityMetadataUpdate(packetEvent);
                } else if(pt.equals(PacketType.Play.Server.ENTITY_DESTROY)) {
                    onEntityDestroy(packetEvent);
                } else if (pt.equals(PacketType.Play.Server.SPAWN_ENTITY)) {
                    StructureModifier<?> integers = packetEvent.getPacket().getIntegers();
                    for(int i=0; i<integers.size(); i++) System.out.println(i+": "+integers.read(i));
                }
            }

            private void onWindowUpdate(PacketEvent packetEvent) {
                boolean legacy = packetEvent.getPacket().getItemArrayModifier().size() > 0;
                List<ItemStack> inventory;
                if(legacy) inventory = Arrays.asList(packetEvent.getPacket().getItemArrayModifier().read(0));
                else inventory = packetEvent.getPacket().getItemListModifier().read(0);
                for(int i=0; i<inventory.size(); i++) {
                    if(isInternal(inventory.get(i))) {
                        ItemStack found = inventory.get(i);
                        inventory.set(i, new ItemStack(Material.AIR));
                        if(legacy) packetEvent.getPacket().getItemArrayModifier().write(0, inventory.toArray(new ItemStack[0]));
                        else packetEvent.getPacket().getItemListModifier().write(0, inventory);
                        if(packetEvent.getPacket().getIntegers().read(0) == 0) {
                            packetEvent.getPlayer().getInventory().remove(found);
                        } else {
                            InventoryView iv = packetEvent.getPlayer().getOpenInventory();
                            if(isInternal(iv.getItem(i))) iv.setItem(i, new ItemStack(Material.AIR));
                            else {
                                iv.getBottomInventory().remove(found);
                                iv.getTopInventory().remove(found);
                            }
                        }
                    }
                }
            }

            private void onSlotSet(PacketEvent packetEvent) {
                ItemStack is = packetEvent.getPacket().getItemModifier().read(0);
                if(isInternal(is)) {
                    int slot = packetEvent.getPacket().getIntegers().read(1);
                    packetEvent.getPacket().getItemModifier().write(0, new ItemStack(Material.AIR));
                    if(packetEvent.getPacket().getIntegers().read(0) == 0) {
                        packetEvent.getPlayer().getInventory().remove(is);
                    } else {
                        InventoryView iv = packetEvent.getPlayer().getOpenInventory();
                        if(slot < iv.countSlots() && isInternal(iv.getItem(slot))) iv.setItem(slot, new ItemStack(Material.AIR));
                        else {
                            iv.getBottomInventory().remove(is);
                            iv.getTopInventory().remove(is);
                        }
                    }
                }
            }

            private int[][] lastRemoving = new int[2][0];
            private void onEntityDestroy(PacketEvent packetEvent) {
                boolean empty;
                synchronized (ANIMATION_INSTANCES) {
                    empty = ANIMATION_INSTANCES.isEmpty();
                }
                if(!empty) {
                    int[] OriginalIds, ids;
                    PacketContainer pc = packetEvent.getPacket();
                    /*for(Method m : pc.getClass().getMethods()) {
                        try{
                            if(m.getParameterCount() > 0) continue;
                            Object o = m.invoke(pc);
                            if(!StructureModifier.class.isInstance(o)) continue;
                            System.out.println(m.getName()+": "+((StructureModifier<?>)o).size());
                        } catch (Throwable ignored) {}
                    }*/
                    if(packetEvent.getPacket().getIntegerArrays().size() == 0) {
                        List<Integer> integers = packetEvent.getPacket().getIntLists().read(0);
                        OriginalIds = new int[integers.size()];
                        for(int i=0; i<integers.size(); i++) OriginalIds[i] = integers.get(i);
                    }
                    else OriginalIds = packetEvent.getPacket().getIntegerArrays().read(0);
                    ids = Arrays.copyOf(OriginalIds, OriginalIds.length);
                    if(Arrays.equals(ids, lastRemoving[0])) {
                        ids = lastRemoving[1];
                        if(ids == null) return;
                        else if(ids.length == 0) packetEvent.setCancelled(true);
                        else packetEvent.getPacket().getIntegerArrays().write(0, ids);
                        return;
                    }
                    boolean modified = false;
                    int i = 0, entityId;
                    searcher:
                    while(i < ids.length) {
                        entityId = ids[i];
                        synchronized (ANIMATION_INSTANCES) {
                            for(AnimationInstance ai:ANIMATION_INSTANCES.values()) {
                                if(ai.collector == null) continue;
                                if(ai.item.getEntityId() == entityId) {
                                    modified = true;
                                    if(ids.length == 1) {
                                        ids = new int[0];
                                        break searcher;
                                    } else {
                                        int[] newIds = new int[ids.length-1];
                                        for(int j=0; j<newIds.length; j++) newIds[j] = ids[j>=i? j+1 : j];
                                        ids = newIds;
                                        continue searcher;
                                    }
                                }
                            }
                        }
                        i++;
                    }
                    lastRemoving = new int[][] {OriginalIds, modified? ids:null};
                    if(modified) {
                        if(ids.length == 0) packetEvent.setCancelled(true);
                        else {
                            packetEvent.getPacket().getIntegerArrays().write(0, ids);
                        }
                    }
                }
            }

            private int lastIdRemoved = -1, lastIdIgnored = -1; // Eficiency flags
            private void onEntityMetadataUpdate(PacketEvent packetEvent) {
                int id = packetEvent.getPacket().getIntegers().read(0);
                if(lastIdIgnored == id) return;
                if(lastIdRemoved == id) {
                    packetEvent.setCancelled(true);
                    return;
                }
                packetEvent.setPacket(packetEvent.getPacket().deepClone());
                StructureModifier<List<WrappedWatchableObject>> modifiers = packetEvent.getPacket().getWatchableCollectionModifier();
                List<? extends AbstractWrapper> modifier;
                if(VersionManager.VERSION >= 19) modifier = packetEvent.getPacket().getDataValueCollectionModifier().read(0);
                else {
                    modifier = packetEvent.getPacket().getWatchableCollectionModifier().read(0);
                    /*for(WrappedWatchableObject wwo : packetEvent.getPacket().getWatchableCollectionModifier().read(0)) {
                        System.out.println(wwo.getIndex()+": "+wwo.getValue()+" ["+wwo.getValue().getClass().getCanonicalName()+"]");
                    }*/
                }
                AnimationInstance ai = null;
                boolean ignored = true;
                AbstractWrapper wwo2 = null, wwo3 = null;
                for(AbstractWrapper wwo : modifier) {
                    int index = WrappedDataProcessor.getIndex(wwo);
                    switch (index) {
                        case 2:
                            wwo2 = wwo;
                            break;
                        case 3:
                            wwo3 = wwo;
                            break;
                        default:
                            boolean wasOptional = false;
                            Object o = WrappedDataProcessor.getValue(wwo);
                            if(o instanceof com.google.common.base.Optional) {
                                com.google.common.base.Optional<?> optional = (com.google.common.base.Optional<?>) o;
                                if(optional.isPresent()) {
                                    o = optional.get();
                                    if(!(o instanceof ItemStack)) {
                                        if(ItemStackUtils.isNMSItemStack(o)) {
                                            o = ItemStackUtils.convertToBukkitItemStack(o);
                                            wasOptional = true;
                                        }
                                    }
                                } else continue;
                            }
                            if(o instanceof ItemStack) {
                                ItemStack is = (ItemStack) o;
                                if(isInternal(is)) {
                                    ignored = false;
                                    synchronized (ANIMATION_INSTANCES) {
                                        ai = ANIMATION_INSTANCES.get(getInternalBookID(is));
                                    }
                                    if(ai != null) {
                                        if(wasOptional) {
                                            WrappedDataProcessor.setValue(wwo, com.google.common.base.Optional.of(ItemStackUtils.convertToNMSItemStack(ai.mask)));
                                        }
                                        else WrappedDataProcessor.setValue(wwo, ai.mask);
                                    } else {
                                        packetEvent.setCancelled(true);
                                        if(lastIdRemoved != id) {
                                            for(World w:Bukkit.getWorlds()) for(Item i:w.getEntitiesByClass(Item.class)) {
                                                if(i.getEntityId() == id) {
                                                    try {
                                                        i.remove();
                                                        lastIdRemoved = id;
                                                    } catch (Throwable ignoredT) {}
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
                if(ignored) lastIdIgnored = id;
                else if(ai != null) {
                    if(wwo2 == null) {
                        if(VersionManager.VERSION >= 19) {
                            List<WrappedDataValue> list = (List<WrappedDataValue>)modifier;
                            list.add(new WrappedDataValue(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), ai.valueFormatted));
                            packetEvent.getPacket().getDataValueCollectionModifier().write(0, list);
                        } else {
                            WrappedDataWatcher wdw = new WrappedDataWatcher();
                            wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), ai.valueFormatted);
                            ((List<WrappedWatchableObject>)modifier).addAll(wdw.getWatchableObjects());
                            packetEvent.getPacket().getWatchableCollectionModifier().write(0, (List<WrappedWatchableObject>)modifier);
                        }
                    } else {
                        if(WrappedDataProcessor.getValue(wwo2) instanceof String) {
                            if(ai.text != null) WrappedDataProcessor.setValue(wwo2, ai.text);
                        }
                        else WrappedDataProcessor.setValue(wwo2, ai.valueFormatted);
                    }

                    if(wwo3 == null) {
                        if(VersionManager.VERSION >= 19) {
                            List<WrappedDataValue> list = (List<WrappedDataValue>)modifier;
                            list.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), ai.valueFormatted.isPresent()));
                            packetEvent.getPacket().getDataValueCollectionModifier().write(0, list);
                        } else {
                            WrappedDataWatcher wdw = new WrappedDataWatcher();
                            wdw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), ai.valueFormatted.isPresent());
                            ((List<WrappedWatchableObject>)modifier).addAll(wdw.getWatchableObjects());
                            packetEvent.getPacket().getWatchableCollectionModifier().write(0, (List<WrappedWatchableObject>)modifier);
                        }
                    } else if(WrappedDataProcessor.getValue(wwo3) instanceof Boolean) WrappedDataProcessor.setValue(wwo3, ai.valueFormatted.isPresent());
                    else if(WrappedDataProcessor.getValue(wwo3) instanceof Byte) WrappedDataProcessor.setValue(wwo3, (byte) (ai.valueFormatted.isPresent()?1:0));
                }
            }

            @Override
            public void onPacketReceiving(PacketEvent packetEvent) {
            }

            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(
                        //PacketType.Play.Server.SPAWN_ENTITY,
                        PacketType.Play.Server.WINDOW_ITEMS,
                        PacketType.Play.Server.SET_SLOT,
                        PacketType.Play.Server.ENTITY_METADATA,
                        PacketType.Play.Server.ENTITY_DESTROY
                ).build();
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

    public static DroppedCoinsAnimation create(ConfigurationSection cs) {
        String format = ColorManager.translateColorCodes(cs.getString("format", ""));
        Expression amountDroppedCalculator = new PreDefinedExpression(cs.getString("amountDrops", "1")).variable("money").build();
        ItemStack[] masks = ItemStackUtils.convert(cs.getStringList("items").toArray(new String[0]));
        MaskOrder order = MaskOrder.valueOf(cs.getString("order", "RANDOM").toUpperCase());
        boolean recollectableByEveryone = cs.getBoolean("recollectableByEveryone", false);
        int autoPickUpInTicks = cs.getInt("autoPickUpInTicks", 0);
        boolean pickUpToPay = cs.getBoolean("pickUpToPay", true);
        boolean silentMessages = cs.getBoolean("silentMessageOnKill", true);
        String messageOnPickUp = ColorManager.translateColorCodes(cs.getString("messageOnPickUp"));
        DefinedSound sound;
        String soundName = cs.getString("sound.name");
        if(soundName != null) {
            float volume = (float)cs.getDouble("sound.volume", 1), pitch = (float)cs.getDouble("sound.pitch", 1);
            try {
                sound = new DefinedSound(Sound.valueOf(soundName.toUpperCase()), volume, pitch);
            } catch (IllegalArgumentException IAEx) {
                MobMoney.sendPluginMessage(ChatColor.RED+"is not a valid sound name! Check your configuration and use https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html as guide! (If you are not using the latest Minecraft version, this guide may be can't help, you can contact support in our Discord server: https://discord.gg/J7Ze4A54K7)");
                return null;
            }
        } else sound = null;
        return new DroppedCoinsAnimation(format, amountDroppedCalculator, masks, order,
                recollectableByEveryone,autoPickUpInTicks, pickUpToPay, silentMessages, messageOnPickUp, sound);
    }

    private static synchronized long generateID() {
        return (System.currentTimeMillis() >>> 10) << 32 | (ITEM_SUBID++);
    }

    private static ItemStack getInternalBook(long id) {
        ItemStack internal = INTERNAL_BOOK.clone();
        BookMeta bm = (BookMeta) internal.getItemMeta();
        List<String> lore = bm.getLore();
        lore.add(String.valueOf(id));
        bm.setLore(lore);
        internal.setItemMeta(bm);
        return internal;
    }

    @SuppressWarnings("DataFlowIssue")
    private static long getInternalBookID(ItemStack book) throws NullPointerException, NumberFormatException {
        List<String> list = (book.getItemMeta()).getLore();
        if(list.size() < 2) return 0;
        return Long.parseLong(list.get(1));
    }

    private static boolean isInternal(ItemStack is) {
        if(is == null) return false;
        else if(is.getType() != Material.WRITTEN_BOOK) return false;
        else {
            BookMeta bm = (BookMeta) is.getItemMeta();
            if(bm == null) return false;
            else if(!INTERNAL_BOOK_META.getAuthor().equals(bm.getAuthor())) return false;
            else if(!INTERNAL_BOOK_META.getPages().equals(bm.getPages())) return false;
            else if(bm.getLore() == null) return false;
            else {
                return bm.getLore().get(0).equals(INTERNAL_BOOK_META.getLore().get(0));
            }
        }
    }

    private final String format, messageOnPickUp;
    private final Expression amountDroppedCalculator;
    private final ItemStack[] masks;
    private final MaskOrder order;
    private final boolean recollectableByEveryone;
    private final int autoPickUpInTicks, flag;
    private final DefinedSound soundOnPickUp;

    public DroppedCoinsAnimation(String format, @Nullable Expression amountDroppedCalculator, ItemStack[] masks,
                                 MaskOrder order, boolean recollectableByEveryone, int autoPickUpInTicks,
                                 boolean pickUpToPay, boolean silentMessages, @Nullable String messageOnPickUp,
                                 @Nullable DefinedSound soundOnPickUp) {
        this.format = format;
        this.amountDroppedCalculator = amountDroppedCalculator;
        this.masks = masks;
        this.order = order;
        this.recollectableByEveryone = recollectableByEveryone;
        this.autoPickUpInTicks = autoPickUpInTicks;
        this.flag = (pickUpToPay? 0b1 : 0) | (silentMessages? 0b10 : 0);
        this.messageOnPickUp = messageOnPickUp;
        this.soundOnPickUp = soundOnPickUp;
    }

    @Override
    public void apply(AsyncMobMoneyEntityKilledEvent e) {
        double reward = e.getFinalReward();
        final Location location = e.getKilledEntity().getLocation();

        int drops;
        if(amountDroppedCalculator == null) drops = 1;
        else synchronized (amountDroppedCalculator) {
            amountDroppedCalculator.setVariable("money", reward);
            drops = Math.max(1, (int) Math.min(amountDroppedCalculator.evaluate(), reward*100));
        }

        double rewardPerItem = Math.round((reward / drops) * 100d) / 100d;
        double itemReward;

        //AnimationInstance[] instances = new AnimationInstance[drops];
        //ItemStack[] masks = new ItemStack[drops];
        AnimationInstance ai;
        CounterByObject counter = new CounterByObject();

        for(int i=0; i<drops; i++) {
            if(i == 0) {
                itemReward = reward - (rewardPerItem * (drops-1));
            } else itemReward = rewardPerItem;

            ItemStack mask;
            switch (order) {
                case ORDERED:
                    mask = masks[i%masks.length];
                    break;
                case RANDOM: default:
                    mask = masks[(int)(Math.random() * masks.length)];
                    break;
            }
            long id = generateID();
            ai = new AnimationInstance(id, mask, itemReward, e.getKiller(), e.getKilledEntity(), counter);
            synchronized (ANIMATION_INSTANCES) {
                ANIMATION_INSTANCES.put(id, ai);
            }
            //masks[i] = getInternalBook(id);
            try {
                ai.item = Bukkit.getScheduler().callSyncMethod(MobMoney.instance, ()->location.getWorld().dropItemNaturally(location, getInternalBook(id))).get();
            } catch (Exception Ex) {
                throw new RuntimeException(Ex);
            }
        }

        /*Bukkit.getScheduler().callSyncMethod(MobMoney.instance, ()->{
            for(int i=0; i<instances.length; i++) {
                instances[i].item = location.getWorld().dropItemNaturally(location, masks[i]);
            }
            return null;
        });*/

    }

    @Override
    public int getFlags() {
        return flag;
    }

    private void destroy(SpawnEntityWrappedPacket spawnPacket, Player p, int stack, World w) {
        PickUpItemWrappedPacket pickUpPacket = new PickUpItemWrappedPacket(p, spawnPacket.getEntityIntID(), stack);
        pickUpPacket.play(w);
    }

    private class AnimationInstance {

        final Long id;
        final ItemStack mask;
        final double value;
        final Player killer;
        String text;
        final Optional<Object> valueFormatted;
        final Entity victim;
        final CounterByObject counter;
        BukkitTask collectorTask;
        Player collector;
        Item item;
        int ticks;

        AnimationInstance(Long id, ItemStack mask, double value, Player killer, Entity victim, CounterByObject counter) {
            this.id = id;
            this.mask = mask;
            this.value = value;
            this.killer = killer;
            this.victim = victim;
            this.counter = counter;
            if(format == null) valueFormatted = Optional.empty();
            else {
                text = format.replace("%money%", MobMoney.eco.format(value)).replace("%value%", String.format("%.2f", value));
                valueFormatted = Optional.of(WrappedChatComponent.fromChatMessage(text)[0].getHandle());
            }
            collectorTask = Bukkit.getScheduler().runTaskTimerAsynchronously(MobMoney.instance, ()->{
                if(item == null) return;
                if(item.isDead()) {
                    synchronized (ANIMATION_INSTANCES) {
                        ANIMATION_INSTANCES.remove(id);
                    }
                    collectorTask.cancel();
                    return;
                }
                if(autoPickUpInTicks > 0 && ++ticks >= autoPickUpInTicks) {
                    pickUp(killer);
                    return;
                }
                if(canCollect(killer)) {
                    pickUp(killer);
                    return;
                }
                if(recollectableByEveryone) for(Player p : item.getWorld().getPlayers()) {
                    if(p.equals(killer)) continue;
                    if(canCollect(p)) {
                        pickUp(p);
                        return;
                    }
                }
            }, 20, 1);
        }

        public void pickUp(Player p) {
            collectorTask.cancel();
            collector = p;
            String entityName = victim.getType().name();
            Mob mob = Mob.getEntity(entityName);
            PickUpItemWrappedPacket pickUpPacket = new PickUpItemWrappedPacket(p, item.getEntityId(), mask.getAmount());
            pickUpPacket.play(p.getWorld());
            if((flag & 0b1) != 0) {
                if(value >= 0) eco.depositPlayer(p, value);
                else if(mob != null && mob.isAllowedNegativeValues()) eco.withdrawPlayer(p, -value);
            }
            synchronized (ANIMATION_INSTANCES) {
                ANIMATION_INSTANCES.remove(id);
            }
            if(soundOnPickUp != null) soundOnPickUp.play(p);
            if(messageOnPickUp != null) {
                if(mob != null) entityName = mob.getName();
                double money;
                synchronized (counter) {
                    money = counter.getStatus(p, value);
                }
                MobMoney.sendMessage(messageOnPickUp
                                .replace("%money%", MobMoney.eco.format(money))
                                .replace("%value%", String.format("%.2f", money))
                                .replace("%entity%", entityName)
                                .replace("%name%", victim.getName()),
                        p
                );
            }
            Bukkit.getScheduler().runTask(MobMoney.instance, ()->item.remove());
        }

        private boolean canCollect(Player p) {
            if(p.getGameMode().equals(GameMode.SPECTATOR)) return false;
            EntityBox bb;
            if(p.getVehicle() != null && !p.getVehicle().isDead()) {
                bb = EntityBox.getFromEntity(p).grow(EntityBox.getFromEntity(p.getVehicle())).grow(1, 0, 1);
            } else bb = EntityBox.getFromEntity(p).grow(1, 0.5, 1);
            return bb.contains(EntityBox.getFromEntity(item));
        }
    }

    public enum MaskOrder {
        ORDERED, RANDOM;
    }

}
