package com.anderhurtado.spigot.mobmoney.objets;

public class Timer{
	public static int TIEMPO,KILLS;

	public int killed=0;
	public long ultimoPeriodo=System.currentTimeMillis();

	public Timer(final User u){
		u.setTimer(this);
	}
	public boolean addEntity(){
		if(ultimoPeriodo+TIEMPO<System.currentTimeMillis()){
		    ultimoPeriodo=System.currentTimeMillis();
		    killed=0;
        }killed++;
		return(killed<=KILLS);
	}
	public boolean canGiveReward(){
		return(killed<=KILLS);
	}
}
