package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.event.AsyncMobMoneyEntityKilledEvent;
import com.anderhurtado.spigot.mobmoney.objets.rewards.RewardAnimation;
import com.anderhurtado.spigot.mobmoney.util.ColorManager;
import com.anderhurtado.spigot.mobmoney.util.PreDefinedExpression;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Mob{

	private static final HashMap<String,Mob> mobs=new HashMap<>();

	public static Mob getEntity(String t){
		return mobs.get(t.toLowerCase());
	}

	public static Mob getEntity(EntityType t) {
		return getEntity(t.name());
	}

	public static void clearMobs(){
	    mobs.clear();
    }

	private static RewardAnimation[] DEFAULT_REWARD_ANIMATIONS;

	public static void setDefaultRewardAnimations(RewardAnimation[] animations) {
		DEFAULT_REWARD_ANIMATIONS = animations;
	}

	private final Expression price;
	private final double defaultLevel;
	private final String entityType;
	private final Expression damageRequired;
	private final HashMap<CreatureSpawnEvent.SpawnReason, Expression> otherPrices = new HashMap<>();
	private String name;
	private RewardAnimation[] rewardAnimations;
	private boolean negativeValues;
	private String customMessageOnKill;
	
	public Mob(String entityType,String price,String name, double defaultLevel, @Nullable Expression damageRequired, boolean negativeValues, @Nullable String customMessageOnKill){
		this.name=name;
		this.defaultLevel = defaultLevel;
		this.entityType = entityType;
		this.price = convertToExpression(price);
		this.damageRequired = damageRequired;
		this.negativeValues = negativeValues;
		if(customMessageOnKill != null) this.customMessageOnKill = ColorManager.translateColorCodes(customMessageOnKill);
		mobs.put(entityType.toLowerCase(),this);
	}

	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name=name;
	}

	private Expression convertToExpression(String formula) {
		ExpressionBuilder expressionBuilder = new PreDefinedExpression(formula);
		expressionBuilder.variables("damage", "MMlevel", "LMlevel");
		if(entityType.equalsIgnoreCase("player")) expressionBuilder.variable("money");
		return expressionBuilder.build();
	}

	public void addFormula(String formula, CreatureSpawnEvent.SpawnReason spawnReason) {
		otherPrices.put(spawnReason, convertToExpression(formula));
	}

	public boolean hasCustomReward(CreatureSpawnEvent.SpawnReason spawnReason) {
		return otherPrices.containsKey(spawnReason);
	}

	public double calculateReward(AsyncMobMoneyEntityKilledEvent e) {
		final Expression price = otherPrices.getOrDefault(e.getSpawnReason(), this.price);
		LivingEntity killed = e.getKilledEntity();
		synchronized (price) {
			if(killed instanceof Player && price.getVariableNames().contains("money")) {
				price.setVariable("money", MobMoney.eco.getBalance((Player)killed));
			}
			double level = defaultLevel;
			if(MobMoney.mythicMobsConnector != null) {
				if(MobMoney.mythicMobsConnector.isMythicMob(killed)) {
					level = MobMoney.mythicMobsConnector.getLevelOfMythicMob(killed);
				}
			}
			price.setVariable("MMlevel", level);

			level = defaultLevel;
			if(MobMoney.levelledMobsConnector != null) {
				level = MobMoney.levelledMobsConnector.getLevel(killed);
			}
			price.setVariable("LMlevel", level);

			price.setVariable("damage", e.getDamagedEntity().getDamageFrom(e.getKiller()));
			if(negativeValues) return price.evaluate();
			else return Math.max(price.evaluate(), 0);
		}
	}

	@Nullable
	public RewardAnimation[] getRewardAnimations() {
		if(rewardAnimations == null) return DEFAULT_REWARD_ANIMATIONS;
		else return rewardAnimations;
	}

	public void setRewardAnimations(RewardAnimation[] animations) {
		this.rewardAnimations = animations;
	}

	public Expression getDamageRequired() {
		return damageRequired;
	}

	public boolean isAllowedNegativeValues() {
		return negativeValues;
	}

	public void setNegativeValues(boolean active) {
		negativeValues = active;
	}

	@Nullable
	public String getCustomMessageOnKill() {
		return customMessageOnKill;
	}

	public void setCustomMessageOnKill(@Nullable String customMessageOnKill) {
		this.customMessageOnKill = customMessageOnKill;
	}

}
