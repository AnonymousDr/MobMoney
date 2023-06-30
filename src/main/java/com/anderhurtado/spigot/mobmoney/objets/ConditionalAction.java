package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.util.PreDefinedExpression;
import com.anderhurtado.spigot.mobmoney.util.function.Max;
import com.anderhurtado.spigot.mobmoney.util.function.Min;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConditionalAction {

    private static final HashMap<EntityType, List<ConditionalAction>> CONDITIONALS = new HashMap<>();

    /**
     * Registers a conditional action
     * @param act Conditional action
     * @param et EntityType. If null, will make a reference for all entity types. If Entity Type is
     *           EntityType.UNKNOWN, will make a reference for all entity types with no registrations
     */
    public static void registerConditional(ConditionalAction act, EntityType et) {
        List<ConditionalAction> list;

        if(CONDITIONALS.containsKey(et)) list = CONDITIONALS.get(et);
        else CONDITIONALS.put(et, list = new ArrayList<>());

        list.add(act);
    }

    public static HashMap<EntityType, List<ConditionalAction>> getConditionals() {
        return CONDITIONALS;
    }

    public static void resetConditionals() {
        CONDITIONALS.clear();
    }

    public static void handleEconomics(AsyncMobMoneyEntityKilledEvent e, int strike) {
        EntityType et = e.getKilledEntity().getType();
        if(CONDITIONALS.containsKey(null)) handleEconomics(CONDITIONALS.get(null), e, strike);
        if(CONDITIONALS.containsKey(et)) handleEconomics(CONDITIONALS.get(et), e, strike);
        else if(CONDITIONALS.containsKey(EntityType.UNKNOWN)) handleEconomics(CONDITIONALS.get(EntityType.UNKNOWN), e, strike);
    }
    private static void handleEconomics(List<ConditionalAction> actions, AsyncMobMoneyEntityKilledEvent e, int strike) {
        actions.forEach(a->{
            if(a.validStrike(strike)) a.updateEconomics(e, strike);
        });
    }

    public static void handleCommands(AsyncMobMoneyEntityKilledEvent e, int strike) {
        EntityType et = e.getKilledEntity().getType();
        Player j = e.getKiller();
        if(CONDITIONALS.containsKey(null)) handleCommands(CONDITIONALS.get(null), j, strike);
        if(CONDITIONALS.containsKey(et)) handleCommands(CONDITIONALS.get(et), j, strike);
        else if(CONDITIONALS.containsKey(EntityType.UNKNOWN)) handleCommands(CONDITIONALS.get(EntityType.UNKNOWN), j, strike);
    }
    private static void handleCommands(List<ConditionalAction> actions, Player j, int strike) {
        Bukkit.getScheduler().callSyncMethod(MobMoney.instance,()->{
            actions.forEach(a->{
                if(a.validStrike(strike)) a.executeCommands(j, strike);
            });
            return null;
        });

    }

    private final int minRequired, maxRequired;
    private final PreconfiguredCommand[] commands;
    private final @Nullable Expression strikeMultiplicator, strikeBase;

    public ConditionalAction(int minRequired, int maxRequired, PreconfiguredCommand[] commands, @Nullable String strikeMultiplicator, @Nullable String strikeBase) {
        this.minRequired = minRequired;
        this.maxRequired = maxRequired;
        this.commands = commands;
        if(strikeMultiplicator != null){
            ExpressionBuilder eb = new PreDefinedExpression(strikeMultiplicator).variable("x");
            if(strikeMultiplicator.contains("y")) eb.variable("y");
            this.strikeMultiplicator = eb.build();
        }
        else this.strikeMultiplicator = null;

        if(strikeBase != null) {
            this.strikeBase = new PreDefinedExpression(strikeBase).variables("x","y").build();
        } else this.strikeBase = null;
    }

    public boolean validStrike(int strike) {
        return maxRequired >= strike && minRequired <= strike;
    }

    public void updateEconomics(AsyncMobMoneyEntityKilledEvent e, int strike) {
        if(!validStrike(strike)) return;
        if(strikeBase != null) synchronized (strikeBase) {
            e.setReward(strikeBase.setVariable("x", strike).setVariable("y", e.getReward()).evaluate());
        }
        if(strikeMultiplicator != null) synchronized (strikeMultiplicator) {
            strikeMultiplicator.setVariable("x", strike);
            if(strikeMultiplicator.getVariableNames().contains("y")) {
                strikeMultiplicator.setVariable("y", e.getMultiplicator());
                e.setMultiplicator(strikeMultiplicator.evaluate());
            } else e.setMultiplicator(e.getMultiplicator() * strikeMultiplicator.evaluate());
        }
    }

    public void executeCommands(Player affected, int strike) {
        if(!validStrike(strike)) return;
        if(commands == null) return;
        String command;
        for(PreconfiguredCommand c:commands) {
            command = c.getCommand()
                    .replace("%player%", affected.getName())
                    .replace("%strike%", String.valueOf(strike));
            switch (c.getExecutionType()) {
                case PLAYER:
                    affected.performCommand(command);
                    break;
                case CONSOLE:
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    break;
                case PLAYEROP:
                    boolean isOP = affected.isOp();
                    if(!isOP) affected.setOp(true);
                    affected.performCommand(command);
                    if(!isOP) affected.setOp(false);
                    break;
            }
        }
    }

}
