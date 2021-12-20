package com.anderhurtado.spigot.mobmoney.objetos;

import java.util.HashMap;
import org.bukkit.entity.EntityType;

public class Mob{

	private static final HashMap<String,Mob> mobs=new HashMap<>();

	public static Mob getEntidad(String t){
		return mobs.get(t.toLowerCase());
	}

	public static void limpiarMobs(){
	    mobs.clear();
    }
	
	private double price;
	private String name;
	
	public Mob(String et,double precio,String nombre){
		mobs.put(et.toLowerCase(),this);
		price=precio;
		name=nombre;
	}
	
	public double getPrice(){
		return price;
	}
	public void setPrice(double precio){
		price=precio;
	}
	public String getName(){
		return name;
	}
	public void setName(String nombre){
		name=nombre;
	}
}
