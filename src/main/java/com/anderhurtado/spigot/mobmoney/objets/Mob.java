package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.objets.rewards.RewardAnimation;
import com.anderhurtado.spigot.mobmoney.util.PreDefinedExpression;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Entity;
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

	public static void clearMobs(){
	    mobs.clear();
    }

	private static RewardAnimation[] DEFAULT_REWARD_ANIMATIONS;/* = new RewardAnimation[] {
			new DroppedCoinsAnimation(net.md_5.bungee.api.ChatColor.of("#660033") +"$"+ChatColor.GREEN+"%value%",
					new PreDefinedExpression("min(50, money/2)").variable("money").build(),
					new ItemStack[] {new ItemStack(Material.DIAMOND), new ItemStack(Material.EMERALD, 2)},
					DroppedCoinsAnimation.MaskOrder.RANDOM, true, 0,
					true, true, ColorManager.translateColorCodes(ChatColor.GREEN+"You've got #999900%money%"+ChatColor.GREEN+" for killing &6")+"%entity%"+ChatColor.GREEN+"!",
					new DefinedSound(Sound.valueOf("BLOCK_NOTE_BLOCK_PLING"), 0.35f, 1.9f)),
			new HologramAnimation(ChatColor.GOLD+"%money%", 0.5f, 100, 1.5),
			new DroppedItemsAnimation(new PreDefinedExpression("maxHealth/2").variable("maxHealth").build(), true, Bukkit.getUnsafe().modifyItemStack(new ItemStack(Material.DIRT),"{Unbreakable:1,EntityTag:{Invisible:1b},display:{Name:'[{\"text\":\"Mi Espada bonita\",\"italic\":false}]',Lore:['[{\"text\":\"Tremenda espada\",\"italic\":false}]','[{\"text\":\"Que he conseguid yo, sabes?\",\"italic\":false}]','[{\"text\":\"¡PUES SÍ!\",\"italic\":false,\"color\":\"dark_aqua\"}]']},Enchantments:[{id:aqua_affinity,lvl:1},{id:bane_of_arthropods,lvl:3},{id:binding_curse,lvl:1},{id:fire_aspect,lvl:1}]}"), new ItemStack(Material.STICK, 2))
	};*/

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
	
	public Mob(String entityType,String price,String name, double defaultLevel, @Nullable Expression damageRequired){
		this.name=name;
		this.defaultLevel = defaultLevel;
		this.entityType = entityType;
		this.price = convertToExpression(price);
		this.damageRequired = damageRequired;
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

	public double calculateReward(Player killer, DamagedEntity damagedVictim) {
		Entity killed = damagedVictim.getEntity();
		final Expression price = otherPrices.getOrDefault(damagedVictim.getSpawnReason(), this.price);
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
			if(MobMoney.levelledMobsConnector != null && killed instanceof LivingEntity) {
				level = MobMoney.levelledMobsConnector.getLevel((LivingEntity) killed);
			}
			price.setVariable("LMlevel", level);

			price.setVariable("damage", damagedVictim.getDamageFrom(killer));
			return price.evaluate();
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

}
