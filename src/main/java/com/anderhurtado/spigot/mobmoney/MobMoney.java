package com.anderhurtado.spigot.mobmoney;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.anderhurtado.spigot.mobmoney.objetos.*;
import com.anderhurtado.spigot.mobmoney.objetos.Mob;
import com.anderhurtado.spigot.mobmoney.util.MetaEntityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

public class MobMoney extends JavaPlugin{
	public static final HashMap<String,String> msg=new HashMap<>();
	public static final List<SpawnReason> spawnban=new ArrayList<>();
	public static final List<String> bannedUUID=new ArrayList<>();
	public static List<String> disabledWorlds;
	public static boolean disableCreative,enableTimer, debug;
	public static File cplugin;
	public static MobMoney instancia;
    public static Economy eco;
	static boolean action;
	static int day;
	public static HashMap<String,Double> dailylimit;
	public static double dailylimitLimit;

    public void onEnable(){
		try{
			instancia=this;
			cplugin=getDataFolder();
			if(!cplugin.exists())cplugin.mkdir();
			setConfig();
			eco=Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            Metrics metrics=new Metrics(this);
			String s=Bukkit.getVersion().split("MC: ")[1].replace(")","");
			if(!(s.startsWith("1")&&Integer.parseInt(s.split("\\.")[1])<13))Bukkit.getPluginManager().registerEvents(new DrownedProtection(),this);
			Bukkit.getPluginManager().registerEvents(new Listener(){
				@EventHandler
				public void alEntrar(PlayerJoinEvent e){
					new User(e.getPlayer().getName());
				}
				@EventHandler
				public void alSalir(PlayerQuitEvent e){
					User u=User.getUser(e.getPlayer().getName());
				}
				@EventHandler
				public void alMorirENTIDAD(EntityDeathEvent e){
					LivingEntity m=e.getEntity();
					String entityType;
					if(m.getType().equals(EntityType.UNKNOWN)) entityType = MetaEntityManager.getEntityType(m);
					else entityType = m.getType().toString();
					if(debug) System.out.println("[MOBMONEY DEBUG] Entity killed: "+entityType);
					if(disabledWorlds.contains(m.getWorld().getName()))return;
					Mob mob=Mob.getEntidad(entityType);
					if(mob==null || mob.getPrice() == 0)return;
					Player j=m.getKiller();
					if(j==null)return;
					if(!j.hasPermission("mobmoney.get"))return;
					if(disableCreative&&j.getGameMode().equals(GameMode.CREATIVE))return;
                    User u=User.getUser(j.getName());
					if(bannedUUID.contains(m.getUniqueId().toString())){
						if(u.getReceiveOnDeath())sendMessage(msg.get("Events.entityBanned"),j);
						return;
					}String uuid=j.getUniqueId().toString();
					if(!u.canGiveReward()){
						if(u.getReceiveOnDeath())sendMessage(msg.get("Events.MaxKillsReached"),j);
						return;
					}if(dailylimit!=null){
                        if(!dailylimit.containsKey(uuid))dailylimit.put(uuid,0d);
					    if(dailylimit.get(uuid)>=dailylimitLimit){
                            if(u.getReceiveOnDeath())sendMessage(msg.get("Events.dailyLimitReached").replace("%limit%",String.valueOf(dailylimitLimit)),j);
                            return;
                        }dailylimit.replace(uuid,dailylimit.get(uuid)+mob.getPrice());
                    }
					if(u.getReceiveOnDeath())sendMessage(msg.get("Events.hunt").replace("%entity%",mob.getName()).replace("%reward%",String.valueOf(mob.getPrice())),j);
                    eco.depositPlayer(j,mob.getPrice());
				}

				@EventHandler
				public void alSpawnear(CreatureSpawnEvent e){
					if(spawnban.contains(e.getSpawnReason())){
					    Entity E=e.getEntity();
					    bannedUUID.add(E.getUniqueId().toString());
					    try{
							for(Entity P:E.getPassengers()) bannedUUID.add(P.getUniqueId().toString());
                        }catch(Throwable Ex){
                            @SuppressWarnings("deprecation")
							Entity P=E.getPassenger();
                            if(P!=null)bannedUUID.add(P.getUniqueId().toString());
                        }
                    }
				}
			},this);
			
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
			
			Bukkit.getScheduler().runTaskLater(this,new Runnable(){
				@Override
				public void run(){
					for(Player j:Bukkit.getOnlinePlayers())new User(j.getName());
				}
			},1);
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
		for(EntityType et:EntityType.values()){
			if(!(et.isSpawnable()&&et.isAlive()))continue;
			String name=et.name().toLowerCase();
			if(!config.contains("Entity.economy."+name))config.set("Entity.economy."+name,1d);
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
		
		//Català
		archivo=new File(idiomas+"/Catalan.yml");
		yml=new YamlConfiguration();
		if(!archivo.exists())archivo.createNewFile();
		else yml.load(archivo);
		setDefault(yml,"Events.hunt","&aHas caçat un &b%entity%&a i has sigut recompensat amb %reward%&a$!");
		setDefault(yml,"Events.MaxKillsReached","&cHas arribat al limit d'entitats que pots matar!");
		setDefault(yml,"Events.entityBanned","&cL'entitat que has caçat es troba banejada.");
        setDefault(yml,"Events.dailyLimitReached","&cHas arribat al limit de diners que pots obtenir avui. (%limit%$)");
		setDefault(yml,"Commands.noPermission","&cNo tens suficients privilegis per a això.");
		setDefault(yml,"Commands.onlyPlayers","&cAquest comand solamment pot ser executat per jugadors dins del joc.");
		setDefault(yml,"Commands.invalidArguments","&cArguments invàlids.");
		setDefault(yml,"Commands.arguments.reload","recarregar");
		setDefault(yml,"Commands.arguments.disableWorld","deshabilitarmon");
		setDefault(yml,"Commands.arguments.enableWorld","habilitarmon");
		setDefault(yml,"Commands.arguments.toggle","toggle");
		setDefault(yml,"Commands.Messages.enabledMessages","&aAra rebràs missatges!");
		setDefault(yml,"Commands.Messages.disabledMessages","&6Ja no rebràs missatges!");
		setDefault(yml,"Commands.Messages.addWorld","&aMón habilitat!");
		setDefault(yml,"Commands.Messages.delWorld","&aMón deshabilitat!");
		setDefault(yml,"Commands.Messages.CurrentlyWorldAdded","&6El món ja estava a la llista!");
		setDefault(yml,"Commands.Messages.WorldNotFinded","&cNo s'ha trobat el món!");
		setDefault(yml,"Commands.Use.reload","&aRecarregar la configuració: &b/mobmoney recarregar");
		setDefault(yml,"Commands.Use.enableWorld","&aHabilitar món: &b/mobmoney habilitarmon <Món>");
		setDefault(yml,"Commands.Use.disableWorld","&aDeshabilitar món: &b/mobmoney deshabilitarmon <Món>");
		setDefault(yml,"Commands.Use.toggle","&aDeshabilitar messatges: &b/mobmoney toggle");
		yml.save(archivo);

        //Dutch
        archivo=new File(idiomas+"/Dutch.yml");
        yml=new YamlConfiguration();
        if(!archivo.exists())archivo.createNewFile();
        else yml.load(archivo);
        setDefault(yml,"Events.hunt","&aJe hebt een &b%entity%&e vermoord en daarvoor heb je %reward%&e punt(en) gekregen!");
        setDefault(yml,"Events.MaxKillsReached","&cYou reached the limit of entities of you can kill!");
        setDefault(yml,"Events.entityBanned","&cDeze mob komt van een spawner! Voor mobs uit spawners staat MobMoney &c&luitgeschakeld&e om puntenfarms te voorkomen.");
        setDefault(yml,"Events.dailyLimitReached","&cJe hebt het maximum aantal punten per dag bereikt! (%limit%$)");
        setDefault(yml,"Commands.noPermission","&cSorry, maar je hebt geen permissies om dit commando uit te voeren.");
        setDefault(yml,"Commands.onlyPlayers","&cDit commando bestaat niet. Type /help voor een lijst met alle commandos.");
        setDefault(yml,"Commands.invalidArguments","&cJe argumenten kloppen niet! Probeer het opnieuw.");
        setDefault(yml,"Commands.arguments.reload","herlaad");
        setDefault(yml,"Commands.arguments.disableWorld","disableworld");
        setDefault(yml,"Commands.arguments.enableWorld","enableworld");
        setDefault(yml,"Commands.arguments.toggle","toggle");
        setDefault(yml,"Commands.Messages.enabledMessages","&aJe hebt berichten voor MobMoney &a&lingeschakeld&e.");
        setDefault(yml,"Commands.Messages.disabledMessages","&6Je hebt berichten voor MobMoney &c&luitgeschakeld&e.");
        setDefault(yml,"Commands.Messages.addWorld","&aWorld enabled!");
        setDefault(yml,"Commands.Messages.delWorld","&aWorld disabled!");
        setDefault(yml,"Commands.Messages.CurrentlyWorldAdded","&6The world was already in the list!");
        setDefault(yml,"Commands.Messages.WorldNotFinded","&cThe world has not been found!");
        setDefault(yml,"Commands.Use.reload","&aHerlaad de configuratie: &e/mobmoney herlaad");
        setDefault(yml,"Commands.Use.enableWorld","&aSchakel een wereld in: &e/mobmoney enableworld <wereld>");
        setDefault(yml,"Commands.Use.disableWorld","&aSchakel een wereld uit: &e/mobmoney disableworld <wereld>");
        setDefault(yml,"Commands.Use.toggle","&aSchakel mob-killberichten in/uit: &e/mobmoney toggle");
        yml.save(archivo);
		
		//Cargando configuración
		disabledWorlds=config.getStringList("disabledWorlds");
		action=config.getBoolean("notificationsInActionBar");
		if(config.getBoolean("dailylimit.enabled")){
		    if(dailylimit!=null)guardarLimiteDiario();
		    day=Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            dailylimitLimit=config.getDouble("dailylimit.limit");
            dailylimit=new HashMap<>();
		    File dl=new File(cplugin+"/dailylimit.dat");
		    FileConfiguration dyml=new YamlConfiguration();
		    if(dl.exists()){
		        dyml.load(dl);
		        if(dyml.getInt("day")==day){
		            ConfigurationSection cs=dyml.getConfigurationSection("users");
		            if(cs!=null)for(String s:cs.getKeys(false))dailylimit.put(s,cs.getDouble(s));
                }
            }else dl.createNewFile();
        }else dailylimit=null;
		File fidioma=new File(idiomas+"/"+config.getString("Language")+".yml");
		if(!fidioma.exists()){
			Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA+"[MobMoney] "+ChatColor.RED+"Language file named '"+fidioma.getName()+"' not found! Using 'English.yml'");
			config.set("Language","English");
			config.save(fConfig);
			fidioma=new File(idiomas+"/English.yml");
		}User.limpiarUsuarios();
		Mob.limpiarMobs();
		ConfigurationSection entities = config.getConfigurationSection("Entity.economy");
		for(String key:entities.getKeys(false)) {
			double price = entities.getDouble(key);
			String name = config.getString("Entity.name."+key, key);
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
		
		//Cargando idioma
		FileConfiguration idioma=new YamlConfiguration();
		idioma.load(fidioma);
		msg.clear();
		for(String v:idioma.getValues(true).keySet())msg.put(v,ChatColor.translateAlternateColorCodes('&',idioma.getString(v)));
	}
	
	public void onDisable(){
		if(!bannedUUID.isEmpty()){
			List<String> bans=new ArrayList<String>();
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
		}if(dailylimit!=null)guardarLimiteDiario();
	}

	private void guardarLimiteDiario(){
        try{
            File dl=new File(cplugin+"/dailylimit.dat");
            if(!dl.exists())dl.createNewFile();
            FileConfiguration yml=new YamlConfiguration();
            setDefault(yml,"day",day);
            for(String s:dailylimit.keySet())setDefault(yml,"users."+s,dailylimit.get(s));
            yml.save(dl);
        }catch(Exception Ex){
            Ex.printStackTrace();
        }
    }
	
	public boolean onCommand(CommandSender j,Command cmd,String label,String[] args){
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
			}User u=User.getUser(j.getName());
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

	public static void sendMessage(String msg,Player j){
        if(action)try{
            HotbarMessager.sendHotBarMessage(j,msg);
        }catch(Exception Ex){
            Ex.printStackTrace();
            j.sendMessage(msg);
        }else j.sendMessage(msg);
    }
}
