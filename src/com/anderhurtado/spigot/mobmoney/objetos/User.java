package com.anderhurtado.spigot.mobmoney.objetos;

import java.util.HashMap;

import com.anderhurtado.spigot.mobmoney.MobMoney;

public class User{
	public static HashMap<String,User> users=new HashMap<String,User>();
	public static User getUser(String s){
		return users.get(s);
	}
	
	private boolean ReceiveOnDeath=true;
	private final String nick;
	private Timer timer;
	
	public User(String s){
		users.put(s,this);
		nick=s;
	}
	public boolean getReceiveOnDeath(){
		return ReceiveOnDeath;
	}
	public void setReceiveOnDeath(boolean b){
		ReceiveOnDeath=b;
	}
	public Timer getTimer(){
		return timer;
	}
	public void setTimer(Timer t){
		timer=t;
	}
	public boolean canGiveReward(){
		return (!MobMoney.enableTimer||(timer==null?new Timer(this).addEntity():timer.addEntity()));
	}
	public void disconnect(){
		users.remove(nick);
	}
}
