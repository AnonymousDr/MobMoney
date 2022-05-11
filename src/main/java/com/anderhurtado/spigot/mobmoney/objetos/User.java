package com.anderhurtado.spigot.mobmoney.objetos;

import java.util.HashMap;
import java.util.UUID;

import com.anderhurtado.spigot.mobmoney.MobMoney;
import com.anderhurtado.spigot.mobmoney.util.UserCache;
import org.bukkit.entity.Player;

public class User{

	private static final UserCache USER_CACHE = UserCache.getInstance();
	public static final HashMap<UUID,User> users=new HashMap<>();
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

	public User(Player p) {
		this(p.getUniqueId());
	}

	public User(UUID u){
		users.put(u,this);
		uuid=u;
		receiveOnDeath = USER_CACHE.receivesMessagesOnKill(this);
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
	public void disconnect(){
		users.remove(uuid);
	}
}
