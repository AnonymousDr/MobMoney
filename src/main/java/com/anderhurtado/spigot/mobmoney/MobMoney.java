package com.anderhurtado.spigot.mobmoney;

import java.io.File;
import java.util.*;

import com.anderhurtado.spigot.mobmoney.objets.*;
import com.anderhurtado.spigot.mobmoney.objets.Mob;
import com.anderhurtado.spigot.mobmoney.objets.Timer;
import com.anderhurtado.spigot.mobmoney.util.EventListener;
import com.anderhurtado.spigot.mobmoney.util.UserCache;
import com.anderhurtado.spigot.mobmoney.util.softdepend.CrackShotConnector;
import com.anderhurtado.spigot.mobmoney.util.softdepend.MyPetsConnector;
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
	public static final List<String> bannedUUID=new ArrayList<>();
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

			String s=Bukkit.getVersion().split("MC: ")[1].replace(")","");
			if(!(s.startsWith("1")&&Integer.parseInt(s.split("\\.")[1])<13))Bukkit.getPluginManager().registerEvents(new com.anderhurtado.spigot.mobmoney.objets.DrownedProtection(),this);
			Bukkit.getPluginManager().registerEvents(new EventListener(),this);
			
			//Cargando entiades baneadas
			File f=new File(cplugin+"/entitybans.dat");
			if(!f.exists())return;
			FileConfiguration yml=new YamlConfiguration();
			yml.load(f);
			List<String> bans=yml.getStringList("Bans");
			for(World w:Bukkit.getWorlds())for(Entity e:w.getEntities()){
				String UUID=e.getUniqueId().toString();
				if(bans.contains(UUID))bannedUUID.add(UUID);
			}
			
			Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.getOnlinePlayers().forEach(com.anderhurtado.spigot.mobmoney.objets.User::new),1);
		}catch(Exception Ex){
			Ex.printStackTrace();
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"Plugin disabled!");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	private void setConfig()throws Exception{
		File fConfig=new File(cplugin+"/config.yml");
		if(!fConfig.exists())fConfig.createNewFile();
		FileConfiguration config=new YamlConfiguration();
		config.load(fConfig);
		
		//Creando configuración
        if(!config.contains("notificationsInActionBar"))config.set("notificationsInActionBar",false);
		if(!config.contains("disabledWorlds"))config.set("disabledWorlds",new String[0]);
		if(!config.contains("debug"))config.set("debug", false);
		if(!config.contains("Language"))config.set("Language","English");
		if(!config.contains("DisableCreative"))config.set("DisableCreative",true);
		if(!config.contains("Timer.enable"))config.set("Timer.enable",false);
		if(!config.contains("Timer.maxKills"))config.set("Timer.maxKills",20);
		if(!config.contains("Timer.resetTimeInSeconds"))config.set("Timer.resetTimeInSeconds",30);
		if(!config.contains("dailylimit.enabled"))config.set("dailylimit.enabled",false);
		if(!config.contains("dailylimit.limit"))config.set("dailylimit.limit",300);
		if(!config.contains("hooks.CrackShot"))config.set("hooks.CrackShot", true);
		if(!config.contains("hooks.MyPets"))config.set("hooks.MyPets", true);
		String name;
		for(EntityType et:EntityType.values()){
			if(!(et.isSpawnable()&&et.isAlive())) {
				if(et != EntityType.PLAYER) continue;
				else{
					if(!config.contains("Entity.params.player.withdrawKilled")) config.set("Entity.params.player.withdrawKilled", true);
					if(!config.contains("Entity.params.player.affectMultiplier")) config.set("Entity.params.player.affectMultiplier", false);
				}
			}
			name=et.name().toLowerCase();
			if(!config.contains("Entity.economy."+name))config.set("Entity.economy."+name, 0d);
			if(!config.contains("Entity.name."+name))config.set("Entity.name."+name,name);
		}for(SpawnReason sr:SpawnReason.values())if(!config.contains("BlockPayEntitiesSpawnedBy."+sr.name()))config.set("BlockPayEntitiesSpawnedBy."+sr.name(),false);
		config.save(fConfig);
		
		//Creando idiomas
		File idiomas=new File(cplugin+"/language/");
		if(!idiomas.exists())idiomas.mkdir();
		
		//English
		File archivo=new File(idiomas+"/English.yml");
		FileConfiguration yml=new YamlConfiguration();
		if(!archivo.exists())archivo.createNewFile();
		else yml.load(archivo);
		setDefault(yml,"Events.hunt","&aYou've hunted a &b%entity%&a and you've been rewarded %reward%&a!");
		setDefault(yml,"Events.MaxKillsReached","&cYou reached the limit of entities of you can kill!");
		setDefault(yml,"Events.entityBanned","&cThe entity you have hunted is banned.");
		setDefault(yml,"Events.dailyLimitReached","&cYou reached the limit of money you can get today. (%limit%$)");
		setDefault(yml,"Events.withdrawnByKill", "&cYou've been killed by &e%player%&c and you've lost &e%reward%&c.");
		setDefault(yml,"Commands.noPermission","&cYou don't have enough privileges for it.");
		setDefault(yml,"Commands.onlyPlayers","&cThis command only can be executed by in-game players.");
		setDefault(yml,"Commands.invalidArguments","&cInvalid arguments.");
		setDefault(yml,"Commands.arguments.reload","reload");
		setDefault(yml,"Commands.arguments.disableWorld","disableworld");
		setDefault(yml,"Commands.arguments.enableWorld","enableworld");
		setDefault(yml,"Commands.arguments.toggle","toggle");
		setDefault(yml,"Commands.Messages.enabledMessages","&aNow you will receive messages!");
		setDefault(yml,"Commands.Messages.disabledMessages","&6Now you will not receive messages!");
		setDefault(yml,"Commands.Messages.addWorld","&aWorld enabled!");
		setDefault(yml,"Commands.Messages.delWorld","&aWorld disabled!");
		setDefault(yml,"Commands.Messages.CurrentlyWorldAdded","&6The world was already in the list!");
		setDefault(yml,"Commands.Messages.WorldNotFinded","&cThe world has not been found!");
		setDefault(yml,"Commands.Use.reload","&aReload config: &b/mobmoney reload");
		setDefault(yml,"Commands.Use.enableWorld","&aEnable world: &b/mobmoney enableworld <World>");
		setDefault(yml,"Commands.Use.disableWorld","&aDisable world: &b/mobmoney disableworld <World>");
		setDefault(yml,"Commands.Use.toggle","&aToggle messages: &b/mobmoney toggle");
		yml.save(archivo);
		
		//Español
		archivo=new File(idiomas+"/Spanish.yml");
		yml=new YamlConfiguration();
		if(!archivo.exists())archivo.createNewFile();
		else yml.load(archivo);
		setDefault(yml,"Events.hunt","&a¡Has cazado un &b%entity%&a y has sido recompensado con %reward%&a$!");
		setDefault(yml,"Events.MaxKillsReached","&c¡Has alcanzado el límite de entidades que puedes matar!");
		setDefault(yml,"Events.entityBanned","&cLa entidad que has cazado se encuentra baneada.");
        setDefault(yml,"Events.dailyLimitReached","&cHas alcanzado el límite de dinero que puedes obtener hoy. (%limit%$)");
		setDefault(yml,"Events.withdrawnByKill", "&cTe ha asesinado &e%player%&c y has perdido &e%reward%&c.");
		setDefault(yml,"Commands.noPermission","&cNo tienes suficientes privilegios para ello.");
		setDefault(yml,"Commands.onlyPlayers","&cEste comando solo puede ser ejecutado por jugadores dentro del juego.");
		setDefault(yml,"Commands.invalidArguments","&cArgumentos inválidos.");
		setDefault(yml,"Commands.arguments.reload","recargar");
		setDefault(yml,"Commands.arguments.disableWorld","deshabilitarmundo");
		setDefault(yml,"Commands.arguments.enableWorld","habilitarmundo");
		setDefault(yml,"Commands.arguments.toggle","toggle");
		setDefault(yml,"Commands.Messages.enabledMessages","&a¡Ahora recibirás mensajes!");
		setDefault(yml,"Commands.Messages.disabledMessages","&6¡Ya no recibirás mensajes!");
		setDefault(yml,"Commands.Messages.addWorld","&a¡Mundo habilitado!");
		setDefault(yml,"Commands.Messages.delWorld","&a¡Mundo deshabilitado!");
		setDefault(yml,"Commands.Messages.CurrentlyWorldAdded","&6¡El mundo ya estaba en la lista!");
		setDefault(yml,"Commands.Messages.WorldNotFinded","&c¡No se ha encontrado el mundo!");
		setDefault(yml,"Commands.Use.reload","&aRecargar la configuración: &b/mobmoney recargar");
		setDefault(yml,"Commands.Use.enableWorld","&aHabilitar mundo: &b/mobmoney habilitarmundo <Mundo>");
		setDefault(yml,"Commands.Use.disableWorld","&aDeshabilitar mundo: &b/mobmoney deshabilitarmundo <Mundo>");
		setDefault(yml,"Commands.Use.toggle","&aDeshabilitar mensajes: &b/mobmoney toggle");
		yml.save(archivo);
		
		//Cargando configuración
		disabledWorlds=config.getStringList("disabledWorlds");
		action=config.getBoolean("notificationsInActionBar");
		if(config.getBoolean("dailylimit.enabled")){
		    if(dailylimit!=null) dailylimit.save();
            dailylimitLimit=config.getDouble("dailylimit.limit");
            dailylimit= com.anderhurtado.spigot.mobmoney.objets.DailyLimit.getInstance();
        }else dailylimit=null;
		File fidioma=new File(idiomas+"/"+config.getString("Language")+".yml");
		if(!fidioma.exists()){
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"Language file named '"+fidioma.getName()+"' not found! Using 'English.yml'");
			config.set("Language","English");
			config.save(fConfig);
			fidioma=new File(idiomas+"/English.yml");
		}
		com.anderhurtado.spigot.mobmoney.objets.User.limpiarUsuarios();
		Mob.clearMobs();
		ConfigurationSection entities = config.getConfigurationSection("Entity.economy");
		assert entities != null;
		for(String key:entities.getKeys(false)) {
			double price = entities.getDouble(key);
			name = config.getString("Entity.name."+key, key);
			new Mob(key, price, name);
		}
		disableCreative=config.getBoolean("DisableCreative");
		enableTimer=config.getBoolean("Timer.enable");
		if(enableTimer){
			Timer.TIEMPO=(int)(config.getDouble("Timer.resetTimeInSeconds")*1000);
			Timer.KILLS=config.getInt("Timer.maxKills");
		}
		debug = config.getBoolean("debug", false);
		spawnban.clear();
		for(SpawnReason sr:SpawnReason.values())if(config.getBoolean("BlockPayEntitiesSpawnedBy."+sr.name()))spawnban.add(sr);
		withdrawFromPlayers = config.getBoolean("Entity.params.player.withdrawKilled");
		affectMultiplierOnPlayers = config.getBoolean("Entity.params.player.affectMultiplier");
		//Cargando idioma
		FileConfiguration idioma=new YamlConfiguration();
		idioma.load(fidioma);
		msg.clear();
		String value;
		for(String v:idioma.getValues(true).keySet()) {
			value = idioma.getString(v);
			if(value != null) msg.put(v,ChatColor.translateAlternateColorCodes('&', value));
		}

		//Loading soft depends
		if(config.getBoolean("hooks.CrackShot")) {
			if(crackShotConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("CrackShot")) try {
					crackShotConnector = new CrackShotConnector();
				} catch (Throwable t) {
					t.printStackTrace();
					Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"This plugin is not able to connect with CrackShot! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else crackShotConnector = null;

		if(config.getBoolean("hooks.MyPets")) {
			if(myPetsConnector == null) {
				if(Bukkit.getPluginManager().isPluginEnabled("MyPet")) try {
					myPetsConnector = new MyPetsConnector();
				} catch (Throwable t) {
					t.printStackTrace();
					Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"This plugin is not able to connect with MyPets! (Report this bug to MobMoney's developer to fix this)");
				}
			}
		} else myPetsConnector = null;

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
			entityLoop:
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
								continue entityLoop;
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
		if(!bannedUUID.isEmpty()){
			List<String> bans=new ArrayList<>();
			for(World w:Bukkit.getWorlds())for(Entity e:w.getEntities()){
				String UUID=e.getUniqueId().toString();
				if(bannedUUID.contains(UUID))bans.add(UUID);
			}if(bans.isEmpty())return;
			try{
				File f=new File(cplugin+"/entitybans.dat");
				if(!f.exists())f.createNewFile();
				FileConfiguration yml=new YamlConfiguration();
				setDefault(yml,"Bans",bans);
				yml.save(f);
			}catch(Exception Ex){
				Ex.printStackTrace();
			}
		}if(dailylimit!=null) dailylimit.save();
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
				setDefault(yml,"disabledWorlds",disabledWorlds);
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
				setDefault(yml,"disabledWorlds",disabledWorlds);
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
		}j.sendMessage(msg.get("Commands.invalidArguments"));
		if(j.hasPermission("mobmoney.reload"))j.sendMessage(msg.get("Commands.Use.reload"));
		if(j.hasPermission("mobmoney.enableworld"))j.sendMessage(msg.get("Commands.Use.enableWorld"));
		if(j.hasPermission("mobmoney.disableworld"))j.sendMessage(msg.get("Commands.Use.disableWorld"));
		if(j.hasPermission("mobmoney.toggle"))j.sendMessage(msg.get("Commands.Use.toggle"));
		return true;
	}

	private void setDefault(FileConfiguration yml,String key,Object value){
	    if(!yml.contains(key))yml.set(key,value);
    }

	public static void sendMessage(String msg,Entity j){
        if(action)try{
            HotbarMessager.sendHotBarMessage(j,msg);
        }catch(Exception Ex){
            Ex.printStackTrace();
            j.sendMessage(msg);
        }else j.sendMessage(msg);
    }
}
