package com.anderhurtado.spigot.mobmoney.objetos;

import java.util.HashMap;
import org.bukkit.entity.EntityType;

public class Mob{
	private static HashMap<EntityType,Mob> mobs=new HashMap<EntityType,Mob>();
	public static Mob getEntidad(EntityType t){
		return mobs.get(t);
	}
	
	
	private double price;
	private String name;
	
	public Mob(EntityType et,double precio,String nombre){
		mobs.put(et,this);
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
