package com.anderhurtado.spigot.mobmoney.objetos;

import org.bukkit.Bukkit;
import com.anderhurtado.spigot.mobmoney.MobMoney;

public class Timer{
	public static int TICKS,KILLS;
	public int killed=0;
	public Timer(final User u){
		u.setTimer(this);
		Bukkit.getScheduler().runTaskLater(MobMoney.instancia,new Runnable(){
			@Override
			public void run(){
				u.setTimer(null);
			}
		},TICKS);
	}
	public boolean addEntity(){
		killed++;
		return(killed<=KILLS);
	}
	public boolean canGiveReward(){
		return(killed<=KILLS);
	}
}
