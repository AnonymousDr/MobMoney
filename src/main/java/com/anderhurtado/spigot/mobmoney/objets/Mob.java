package com.anderhurtado.spigot.mobmoney.objets;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.util.function.Decode;
import com.anderhurtado.spigot.mobmoney.util.function.Max;
import com.anderhurtado.spigot.mobmoney.util.function.Min;
import com.anderhurtado.spigot.mobmoney.util.softdepend.LevelledMobsConnector;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;

public class Mob{

	private static final HashMap<String,Mob> mobs=new HashMap<>();

	public static Mob getEntity(String t){
		return mobs.get(t.toLowerCase());
	}

	public static void clearMobs(){
	    mobs.clear();
    }
	
	private final Expression price;
	private final double defaultLevel;
	private final String entityType;
	private final HashMap<CreatureSpawnEvent.SpawnReason, Expression> otherPrices = new HashMap<>();
	private String name;
	
	public Mob(String entityType,String price,String name, double defaultLevel){
		this.name=name;
		this.defaultLevel = defaultLevel;
		this.entityType = entityType;
		this.price = convertToExpression(price);
		mobs.put(entityType.toLowerCase(),this);
	}

	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name=name;
	}

	private Expression convertToExpression(String formula) {
		ExpressionBuilder expressionBuilder = new ExpressionBuilder(formula);
		expressionBuilder.functions(Max.getInstance(), Min.getInstance(), Decode.getInstance()).variables("damage", "MMlevel", "LMlevel");
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
}
