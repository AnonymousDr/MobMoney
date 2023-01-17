package com.anderhurtado.spigot.mobmoney.objets;

import java.util.HashMap;

public class Mob{

	private static final HashMap<String,Mob> mobs=new HashMap<>();

	public static Mob getEntity(String t){
		return mobs.get(t.toLowerCase());
	}

	public static void clearMobs(){
	    mobs.clear();
    }
	
	private double price;
	private String name;
	
	public Mob(String entityType,double price,String name){
		mobs.put(entityType.toLowerCase(),this);
		this.price=price;
		this.name=name;
	}
	
	public double getPrice(){
		return price;
	}
	public void setPrice(double price){
		this.price=price;
	}
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name=name;
	}
}
