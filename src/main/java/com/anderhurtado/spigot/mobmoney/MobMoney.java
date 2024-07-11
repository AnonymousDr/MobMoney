package com.anderhurtado.spigot.mobmoney;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

import com.anderhurtado.spigot.mobmoney.objets.*;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.Timer;
import com.anderhurtado.spigot.mobmoney.objets.rewards.RewardAnimation;
import com.anderhurtado.spigot.mobmoney.util.*;
import com.anderhurtado.spigot.mobmoney.util.EventListener;
import com.anderhurtado.spigot.mobmoney.util.softdepend.*;
import net.objecthunter.exp4j.Expression;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

public class MobMoney extends JavaPlugin{
	public static final HashMap<String,String> msg=new HashMap<>();
	public static final List<SpawnReason> spawnban=new ArrayList<>();
	public static List<String> disabledWorlds;
	public static boolean disableCreative,enableTimer, debug, withdrawFromPlayers, affectMultiplierOnPlayers;
	public static File cplugin;
	public static MobMoney instance;
    public static Economy eco;
	static boolean action;
	public static com.anderhurtado.spigot.mobmoney.objets.DailyLimit dailylimit;
	public static double dailylimitLimit;
	public static CrackShotConnector crackShotConnector;
	public static MyPetsConnector myPetsConnector;
	public static MythicMobsConnector mythicMobsConnector;
	public static LevelledMobsConnector levelledMobsConnector;
	public static AuraMobsConnector auraMobsConnector;
	public static long multiplicatorExpiration;
	public static float multiplicatorValue;

	public static void sendPluginMessage(String msg) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+msg);
	}

    public void onEnable(){
		try{
			instance =this;
			cplugin=getDataFolder();
			if(!cplugin.exists())cplugin.mkdir();
			setConfig();
			//Cargando necesarias
			//noinspection ConstantConditions
			eco=Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();

            com.anderhurtado.spigot.mobmoney.objets.Metrics metrics=new com.anderhurtado.spigot.mobmoney.objets.Metrics(this);
			metrics.addCustomChart(new Metrics.SimplePie("using_the_strikesystem", ()->ConditionalAction.getConditionals().isEmpty()?"No":"Yes"));
			Bukkit.getPluginManager().registerEvents(new EventListener(),this);
			
			Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(com.anderhurtado.spigot.mobmoney.objets.User::new),1);
		}catch(Exception Ex){
			Ex.printStackTrace();
			sendPluginMessage(ChatColor.RED+"Plugin disabled!");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	private void setConfig()throws Exception{
		if(!new File(cplugin, "strikeSystem.sample.yml").exists()) saveResource("strikeSystem.sample.yml", false);
		File fConfig=new File(cplugin, "config.yml");

		//Creando configuración
		if(!fConfig.exists()) saveResource("config.yml", true);
		reloadConfig();
		FileConfiguration config = getConfig();

		//Cargando mobs
		File fMobs = new File(cplugin, "mobs.yml");
		if(!fMobs.exists()) fMobs.createNewFile();
		FileConfiguration mobs=new YamlConfiguration();
		mobs.load(fMobs);

		ConfigurationSection entities;
		if(mobs.isConfigurationSection("entities")) entities = mobs.getConfigurationSection("entities");
		else entities = mobs.createSection("entities");
		assert entities != null;

		String name;
		for(EntityType et:EntityType.values()){
			if(!(et.isSpawnable()&&et.isAlive())) {
				if(et != EntityType.PLAYER) continue;
				else{
					if(!entities.contains("player.withdrawMoney")) entities.set("player.withdrawMoney", config.get("Entity.params.player.withdrawKilled", true));
					if(!entities.contains("player.affectMultiplier")) entities.set("player.affectMultiplier", config.get("Entity.params.player.affectMultiplier", false));
				}
			}
			name=et.name().toLowerCase();
			if(!entities.contains(name.concat(".money"))) entities.set(name.concat(".money"), config.get("Entity.economy.".concat(name), 0d));
			if(!entities.contains(name.concat(".name"))) entities.set(name.concat(".name"), config.get("Entity.name.".concat(name), name));
		}
		for(SpawnReason sr:SpawnReason.values()) {
			if(!mobs.contains("general.payEntitiesSpawnedBy.".concat(sr.name().toLowerCase()))) {
				mobs.set("general.payEntitiesSpawnedBy.".concat(sr.name().toLowerCase()), !config.getBoolean("BlockPayEntitiesSpawnedBy.".concat(sr.name()), false));
			}
		}
		mobs.save(fMobs);

		File base = new File(cplugin, "language");
		// Loading languages
		if(!new File(base, "English.yml").exists()) saveResource("language/English.yml", false);
		if(!new File(base, "Spanish.yml").exists()) saveResource("language/Spanish.yml", false);
		if(!new File(base, "Dutch.yml").exists()) saveResource("language/Dutch.yml", false);
		if(!new File(base, "Chinese.yml").exists()) saveResource("language/Chinese.yml", false);
		if(!new File(base, "Catalan.yml").exists()) saveResource("language/Catalan.yml", false);
		if(!new File(base, "Valencian.yml").exists()) saveResource("language/Valencian.yml", false);
		if(!new File(base, "French.yml").exists()) saveResource("language/French.yml", false);
		if(!new File(base, "Russian.yml").exists()) saveResource("language/Russian.yml", false);
		if(!new File(base, "Turkish.yml").exists()) saveResource("language/Turkish.yml", false);
		
		// Loading config
		disabledWorlds = config.getStringList("disabledWorlds");
		action = config.getBoolean("notificationsInActionBar");
		if(config.getBoolean("dailylimit.enabled")){
		    if(dailylimit!=null) dailylimit.save();
            dailylimitLimit=config.getDouble("dailylimit.limit");
            dailylimit= com.anderhurtado.spigot.mobmoney.objets.DailyLimit.getInstance();
        }else dailylimit=null;
		File idiomas=new File(cplugin+"/language/");
		File fidioma=new File(idiomas+"/"+config.getString("Language")+".yml");
		if(!fidioma.exists()){
			sendPluginMessage(ChatColor.RED+"Language file named '"+fidioma.getName()+"' not found! Using 'English.yml'");
			config.set("Language","English");
			saveConfig();
			fidioma=new File(idiomas+"/English.yml");
		}
		com.anderhurtado.spigot.mobmoney.objets.User.limpiarUsuarios();
		Mob.clearMobs();
		Mob.setDefaultRewardAnimations(RewardAnimation.processConfig(config.getConfigurationSection("defaultAnimations")));
		for(String key:entities.getKeys(false)) {
			String price = entities.getString(key.concat(".money"));
			name = ColorManager.translateColorCodes(entities.getString(key.concat(".name"), key));
			Expression damageRequired;
			String damageRequiredStr = entities.getString(key.concat(".minDamage"));
			if(damageRequiredStr == null) damageRequired = null;
			else damageRequired = new PreDefinedExpression(damageRequiredStr).variable("maxHealth").build();
			Mob mob = new Mob(key, price, name, entities.getDouble(key.concat(".defaultLevel"), 1), damageRequired,
					entities.getBoolean(key.concat(".negativeValues"), false), entities.getString(key.concat(".customMessageOnKill")));
			ConfigurationSection otherPrices = entities.getConfigurationSection(key.concat(".whenSpawnedBy"));
			if(otherPrices != null) {
				for(String sprs:otherPrices.getKeys(false)) {
					try {
						mob.addFormula(otherPrices.getString(sprs), SpawnReason.valueOf(sprs));
					} catch (IllegalArgumentException IAEx) {
						sendPluginMessage(ChatColor.RED+sprs +" is not a valid spawn reason! Check your mobs.yml and use https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html as guide! (If you are not using the latest Minecraft version, this guide may be can't help, you can contact support in our Discord server: https://discord.gg/J7Ze4A54K7)");
					}
				}
			}
			mob.setRewardAnimations(RewardAnimation.processConfig(entities.getConfigurationSection(key.concat(".animations"))));
		}
		disableCreative=config.getBoolean("DisableCreative");
		enableTimer=config.getBoolean("Timer.enable");
		if(enableTimer){
			Timer.TIEMPO=(int)(config.getDouble("Timer.resetTimeInSeconds")*1000);
			Timer.KILLS=config.getInt("Timer.maxKills");
		}
		debug = config.getBoolean("debug", false);
		spawnban.clear();
		for(SpawnReason sr:SpawnReason.values()) if(!mobs.getBoolean("general.payEntitiesSpawnedBy."+sr.name().toLowerCase(), true)) spawnban.add(sr);
		withdrawFromPlayers = entities.getBoolean("player.withdrawMoney");
		affectMultiplierOnPlayers = entities.getBoolean("player.affectMultiplier");
		//Cargando idioma
		FileConfiguration idioma=new YamlConfiguration();
		idioma.load(fidioma);
		msg.clear();
		String value;
		for(String v:idioma.getValues(true).keySet()) {
			value = idioma.getString(v);
			if(value != null) msg.put(v,ColorManager.translateColorCodes(value));
		}
		multiplicatorExpiration = config.getLong("temporalMultiplicator.expiration");
		multiplicatorValue = (float)config.getDouble("temporalMultiplicator.value", 1);

		//Loading soft depends
		if(config.getBoolean("hooks.CrackShot")) {
			if(crackShotConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("CrackShot")) try {
					crackShotConnector = new CrackShotConnector();
					sendPluginMessage(ChatColor.GREEN+"Crackshot detected and connected!");
				} catch (Throwable t) {
					t.printStackTrace();
					sendPluginMessage(ChatColor.RED+"This plugin is not able to connect with CrackShot! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else crackShotConnector = null;

		if(config.getBoolean("hooks.MyPets")) {
			if(myPetsConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("MyPet")) try {
					myPetsConnector = new MyPetsConnector();
					sendPluginMessage(ChatColor.GREEN+"MyPet detected and connected!");
				} catch (Throwable t) {
					t.printStackTrace();
					sendPluginMessage(ChatColor.RED+"This plugin is not able to connect with MyPets! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else myPetsConnector = null;

		if(config.getBoolean("hooks.MythicMobs")) {
			if(myPetsConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) try {
					mythicMobsConnector = MythicMobsConnector.getInstance();
					sendPluginMessage(ChatColor.GREEN+"MythicMobs detected and connected!");
				} catch (Throwable t) {
					t.printStackTrace();
					sendPluginMessage(ChatColor.RED+"This plugin is not able to connect with MythicMobs! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else mythicMobsConnector = null;

		if(config.getBoolean("hooks.LevelledMobs")) {
			if(levelledMobsConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("LevelledMobs")) try {
					levelledMobsConnector = new LevelledMobsConnector();
					sendPluginMessage(ChatColor.GREEN+"LevelledMobs detected and connected!");
				} catch (Throwable t) {
					t.printStackTrace();
					sendPluginMessage(ChatColor.RED+"This plugin is not able to connect with LevelledMobs! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else levelledMobsConnector = null;
		if(config.getBoolean("hooks.PlaceholderAPI",true)) {
			if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new PlaceholderConnector().register();
		}

		if(config.getBoolean("hooks.AuraMobs", true)) {
			if(Bukkit.getPluginManager().isPluginEnabled("AuraMobs")) auraMobsConnector = new AuraMobsConnector();
		}

		ConditionalAction.resetConditionals();
		File strikeSystemFile = new File(cplugin, "strikeSystem.yml");
		if(strikeSystemFile.exists()) {
			YamlConfiguration strikeSystem = new YamlConfiguration();
			strikeSystem.load(strikeSystemFile);

			EntityType et;
			ConfigurationSection entitySection, conditionSection, commandsSection, commandSection;
			int minRequired, maxRequired;
			String baseFunction, multiplicatorFunction, command, executeCommandAs;
			final List<PreconfiguredCommand> pcs = new ArrayList<>();
			for(String entityKey:strikeSystem.getKeys(false)) {
				try{
					switch (entityKey.toUpperCase()) {
						case "ALL":
							et = null;
							break;
						case "DEFAULT":
							et = EntityType.UNKNOWN;
							break;
						default:
							try{
								et = EntityType.valueOf(entityKey.toUpperCase());
							} catch (Exception Ex) {
								getLogger().log(Level.WARNING, "No entty found for type "+entityKey);
								continue;
							}
					}
					entitySection = strikeSystem.getConfigurationSection(entityKey);
					assert entitySection != null;
					if(entitySection.contains("maxTime")) StrikeSystem.setMaxTime(et, entitySection.getInt("maxTime"));
					for(String condition:entitySection.getKeys(false)) {
						if(condition.equalsIgnoreCase("maxTime")) continue;
						try{
							conditionSection = entitySection.getConfigurationSection(condition);
							assert conditionSection != null;
							minRequired = conditionSection.getInt("minRequired", 0);
							maxRequired = conditionSection.getInt("maxRequired", Integer.MAX_VALUE);
							baseFunction = conditionSection.getString("baseFunction");
							multiplicatorFunction = conditionSection.getString("multiplicatorFunction");
							pcs.clear();
							commandsSection = conditionSection.getConfigurationSection("commands");
							if(commandsSection != null) {
								for(String commands:commandsSection.getKeys(false)) {
									try{
										commandSection = commandsSection.getConfigurationSection(commands);
										if(commandSection == null) continue;
										command = commandSection.getString("command");
										executeCommandAs = commandSection.getString("executeAs");
										pcs.add(new PreconfiguredCommand(command, PreconfiguredCommand.ExecutionType.getByName(executeCommandAs)));
									} catch (Exception Ex) {
										Ex.printStackTrace();
									}
								}
							}
							ConditionalAction.registerConditional(new ConditionalAction(minRequired, maxRequired, pcs.toArray(new PreconfiguredCommand[0]), multiplicatorFunction, baseFunction), et);
						} catch (Exception Ex) {
							Ex.printStackTrace();
						}
					}
				} catch (Exception Ex) {
					Ex.printStackTrace();
				}
			}
		}
	}
	
	public void onDisable(){
		if(dailylimit!=null) dailylimit.save();
		UserCache.getInstance().save();
	}

	@Override
	public boolean onCommand(CommandSender j, Command cmd, String label, String[] args){
		if(args.length==0){
			boolean permiso=false;
			if(j.hasPermission("mobmoney.reload")&&(permiso=true))j.sendMessage(msg.get("Commands.Use.reload"));
			if(j.hasPermission("mobmoney.enableworld")&&(permiso=true))j.sendMessage(msg.get("Commands.Use.enableWorld"));
			if(j.hasPermission("mobmoney.disableworld")&&(permiso=true))j.sendMessage(msg.get("Commands.Use.disableWorld"));
			if(j.hasPermission("mobmoney.toggle")&&(permiso=true))j.sendMessage(msg.get("Commands.Use.toggle"));
			if(j.hasPermission("mobmoney.temporalMultiplier.set")) j.sendMessage(msg.getOrDefault("Commands.Use.temporalMultiplier", ChatColor.GREEN+"Set a temporal multiplier:"+ChatColor.AQUA+" /mobmoney multiplier <Duration:[1d23h15m31s]> <Multiplier:1.5>"));
			if(!permiso)j.sendMessage(msg.get("Commands.noPermission"));
			return true;
		}String arg0=args[0];
		if(arg0.equalsIgnoreCase(msg.get("Commands.arguments.reload"))){
			if(!j.hasPermission("mobmoney.reload")){
				j.sendMessage(msg.get("Commands.noPermission"));
				return true;
			}try{
				setConfig();
			}catch(Exception Ex){
				Ex.printStackTrace();
				j.sendMessage(ChatColor.RED+"An error ocurred.");
				return true;
			}j.sendMessage(ChatColor.GREEN+"Reloaded!");
			return true;
		}if(arg0.equalsIgnoreCase(msg.get("Commands.arguments.disableWorld"))){
			if(!j.hasPermission("mobmoney.disableworld")){
				j.sendMessage(msg.get("Commands.noPermission"));
				return true;
			}if(args.length==1){
				j.sendMessage(msg.get("Commands.Use.disableWorld"));
				return true;
			}World w=Bukkit.getWorld(args[1]);
			if(w==null){
				j.sendMessage(msg.get("Commands.Messages.WorldNotFinded"));
				return true;
			}if(disabledWorlds.contains(w.getName())){
				j.sendMessage(msg.get("Commands.Messages.CurrentlyWorldAdded"));
				return true;
			}disabledWorlds.add(w.getName());
			try{
				File f=new File(cplugin+"/config.yml");
				FileConfiguration yml=new YamlConfiguration();
				yml.load(f);
				yml.set("disabledWorlds", disabledWorlds);
				yml.save(f);
				j.sendMessage(msg.get("Commands.Messages.delWorld"));
			}catch(Exception Ex){
				j.sendMessage(ChatColor.RED+"An error ocurred");
			}return true;
		}if(arg0.equalsIgnoreCase(msg.get("Commands.arguments.enableWorld"))){
			if(!j.hasPermission("mobmoney.enableworld")){
				j.sendMessage(msg.get("Commands.noPermission"));
				return true;
			}if(args.length==1){
				j.sendMessage(msg.get("Commands.Use.enableWorld"));
				return true;
			}World w=Bukkit.getWorld(args[1]);
			if(w==null){
				j.sendMessage(msg.get("Commands.Messages.WorldNotFinded"));
				return true;
			}if(!disabledWorlds.contains(w.getName())){
				j.sendMessage(msg.get("Commands.Messages.CurrentlyWorldAdded"));
				return true;
			}disabledWorlds.remove(w.getName());
			try{
				File f=new File(cplugin+"/config.yml");
				FileConfiguration yml=new YamlConfiguration();
				yml.load(f);
				yml.set("disabledWorlds", disabledWorlds);
				yml.save(f);
				j.sendMessage(msg.get("Commands.Messages.addWorld"));
			}catch(Exception Ex){
				j.sendMessage(ChatColor.RED+"An error ocurred");
			}return true;
		}if(arg0.equalsIgnoreCase(msg.get("Commands.arguments.toggle"))){
			if(!j.hasPermission("mobmoney.toggle")){
				j.sendMessage(msg.get("Commands.noPermission"));
				return true;
			}if(!(j instanceof Player)){
				j.sendMessage(msg.get("Commands.onlyPlayers"));
				return true;
			}
			com.anderhurtado.spigot.mobmoney.objets.User u= com.anderhurtado.spigot.mobmoney.objets.User.getUser(((Player) j).getUniqueId());
			if(u.getReceiveOnDeath()){
				u.setReceiveOnDeath(false);
				j.sendMessage(msg.get("Commands.Messages.disabledMessages"));
			}else{
				u.setReceiveOnDeath(true);
				j.sendMessage(msg.get("Commands.Messages.enabledMessages"));
			}return true;
		}
		if(arg0.equalsIgnoreCase(msg.getOrDefault("Commands.arguments.temporalMultiplier", "multiplier"))) {
			if(!j.hasPermission("mobmoney.temporalMultiplier")) {
				j.sendMessage(msg.get("Commands.noPermission"));
				return true;
			}
			if(args.length == 1) {
				if(multiplicatorExpiration > System.currentTimeMillis()) {
					String time = TimeUtils.convertToString(multiplicatorExpiration-System.currentTimeMillis());
					j.sendMessage(msg.getOrDefault("Commands.Messages.temporalMultiplierTimeAvailable",ChatColor.GREEN+"Remaining time of %multiplicator%x available: %time%").replace("%multiplicator%",String.valueOf(Math.round(multiplicatorValue*100)/100F)).replace("%time%",time));
				} else {
					j.sendMessage(msg.getOrDefault("Commands.Messages.temporalMultiplierNotAvailable",ChatColor.RED+"There is not temporal multiplier at this moment."));
				}
				return true;
			} else {
				if(!j.hasPermission("mobmoney.temporalMultiplier.set")) {
					j.sendMessage(msg.get("Commands.noPerimssion"));
					return true;
				}
				if(args.length < 3) {
					j.sendMessage(msg.get("Commands.invalidArguments"));
					j.sendMessage(msg.getOrDefault("Commands.Use.temporalMultiplier", ChatColor.GREEN+"Set a temporal multiplier:"+ChatColor.AQUA+" /mobmoney multiplier <Duration:[1d23h15m31s]> <Multiplier:1.5>"));
					return true;
				}
				long expiration;
				try {
					expiration = TimeUtils.convertToTime(args[1]);
					if(expiration <= 0) throw new RuntimeException();
					expiration += System.currentTimeMillis();
				} catch (Exception Ex) {
					j.sendMessage(msg.get("Commands.invalidArguments"));
					j.sendMessage(msg.getOrDefault("Commands.Use.temporalMultiplier", ChatColor.GREEN+"Set a temporal multiplier:"+ChatColor.AQUA+" /mobmoney multiplier <Duration:[1d23h15m31s]> <Multiplier:1.5>"));
					return true;
				}
				float multiplicator;
				try {
					multiplicator = Float.parseFloat(args[2]);
				} catch (Exception Ex) {
					j.sendMessage(msg.get("Commands.invalidArguments"));
					j.sendMessage(msg.getOrDefault("Commands.Use.temporalMultiplier", ChatColor.GREEN+"Set a temporal multiplier:"+ChatColor.AQUA+" /mobmoney multiplier <Duration:[1d23h15m31s]> <Multiplier:1.5>"));
					return true;
				}
				multiplicatorExpiration = expiration;
				multiplicatorValue = multiplicator;
				reloadConfig();
				FileConfiguration config = getConfig();
				config.set("temporalMultiplicator.expiration", expiration);
				config.set("temporalMultiplicator.value", multiplicator);
				saveConfig();
				j.sendMessage(msg.getOrDefault("Commands.Messages.temporalMultiplierSet",ChatColor.GREEN+"The temporal multiplier has been defined!"));
				return true;
			}
		}
		j.sendMessage(msg.get("Commands.invalidArguments"));
		if(j.hasPermission("mobmoney.reload"))j.sendMessage(msg.get("Commands.Use.reload"));
		if(j.hasPermission("mobmoney.enableworld"))j.sendMessage(msg.get("Commands.Use.enableWorld"));
		if(j.hasPermission("mobmoney.disableworld"))j.sendMessage(msg.get("Commands.Use.disableWorld"));
		if(j.hasPermission("mobmoney.toggle"))j.sendMessage(msg.get("Commands.Use.toggle"));
		if(j.hasPermission("mobmoney.temporalMultiplier.set")) j.sendMessage(msg.getOrDefault("Commands.Use.temporalMultiplier", ChatColor.GREEN+"Set a temporal multiplier:"+ChatColor.AQUA+" /mobmoney multiplier <Duration:[1d23h15m31s]> <Multiplier:1.5>"));
		return true;
	}

	private void setDefault(FileConfiguration yml,String key,Object value){
	    if(!yml.contains(key))yml.set(key,value);
    }

	public static void sendMessage(String msg,Entity j){
		if(msg == null || msg.isEmpty()) return;
        if(action)try{
            HotbarMessager.sendHotBarMessage(j,msg);
        }catch(Exception Ex){
            Ex.printStackTrace();
            j.sendMessage(msg);
        }else j.sendMessage(msg);
    }
}
