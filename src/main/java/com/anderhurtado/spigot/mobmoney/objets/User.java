package com.anderhurtado.spigot.mobmoney.objets;

import java.util.HashMap;
import java.util.UUID;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.util.UserCache;
import com.sun.istack.internal.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class User{

	private static final UserCache USER_CACHE = UserCache.getInstance();
	public static final HashMap<UUID,User> users=new HashMap<>();

	private final double multiplicator;
	public static User getUser(UUID uuid){
	    User u=users.get(uuid);
	    if(u==null)u=new User(uuid);
		return u;
	}

	public static void limpiarUsuarios(){
	    users.clear();
    }
	
	private boolean receiveOnDeath;
	public final UUID uuid;
	private Timer timer;
	private final StrikeSystem strikeSystem = new StrikeSystem();

	public User(Player p) {
		this(p.getUniqueId());
	}

	public User(UUID u){
		users.put(u,this);
		uuid=u;
		receiveOnDeath = USER_CACHE.receivesMessagesOnKill(this);
		multiplicator = calculateMultiplicator();
	}

	public double getMultiplicator() {
		return multiplicator;
	}

	private double calculateMultiplicator() {
		Player p = Bukkit.getPlayer(uuid);
		double multiplicator = 1;
		if(p != null) {
			String permission;
			for(PermissionAttachmentInfo perm:p.getEffectivePermissions()) {
				permission = perm.getPermission();
				if(permission.startsWith("mobmoney.multiplicator.")) {
					permission = permission.substring(23);
					try{
						if(multiplicator == 1) multiplicator = Double.parseDouble(permission);
						else multiplicator = Math.max(multiplicator, Double.parseDouble(permission));
					} catch (Exception ignored) {}
				}
			}
		}
		return multiplicator;
	}

	public boolean getReceiveOnDeath(){
		return receiveOnDeath;
	}

	public void setReceiveOnDeath(boolean b){
		receiveOnDeath=b;
		USER_CACHE.setReceivingMessagesOnKill(this, receiveOnDeath);
	}

	public Timer getTimer(){
		return timer;
	}
	protected void setTimer(Timer t){
		timer=t;
	}

	public boolean canGiveReward(){
		return (!MobMoney.enableTimer||(timer==null?new Timer(this):timer).addEntity());
	}

	@NotNull
	public StrikeSystem getStrikeSystem() {
		return strikeSystem;
	}

	public void disconnect(){
		users.remove(uuid);
	}
}
